package com.bugdna.ingest;

import com.bugdna.bug.*;
import com.bugdna.event.Event;
import com.bugdna.event.EventRepository;
import com.bugdna.event.EventType;
import com.bugdna.event.TrackingSession;
import com.bugdna.event.TrackingSessionRepository;
import com.bugdna.ingest.dto.IngestEventDto;
import com.bugdna.project.Project;
import com.bugdna.project.ProjectRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Handles deterministic bug grouping, occurrence creation, and regression detection.
 *
 * Why separate bean with REQUIRES_NEW:
 * When 20 concurrent requests report the exact same brand-new error simultaneously,
 * multiple threads may see "no bug yet" and attempt INSERT. MySQL's UNIQUE(project_id, fingerprint)
 * guarantees only one INSERT succeeds while others throw DataIntegrityViolationException.
 * Because JPA marks a failed transaction rollback-only, isolating each upsert attempt in
 * REQUIRES_NEW lets the caller catch the constraint race and immediately retry, where the
 * retry acquires SELECT ... FOR UPDATE on the committed bug row and increments occurrence_count.
 */
@Service
public class BugGroupingService {

    public record GroupingOutcome(Long bugId, boolean created, boolean updated, boolean regression) {}

    private final BugRepository bugRepository;
    private final BugOccurrenceRepository occurrenceRepository;
    private final EventRepository eventRepository;
    private final ProjectRepository projectRepository;
    private final TrackingSessionRepository sessionRepository;
    private final FingerprintService fingerprintService;
    private final EntityManager entityManager;

    public BugGroupingService(
            BugRepository bugRepository,
            BugOccurrenceRepository occurrenceRepository,
            EventRepository eventRepository,
            ProjectRepository projectRepository,
            TrackingSessionRepository sessionRepository,
            FingerprintService fingerprintService,
            EntityManager entityManager
    ) {
        this.bugRepository = bugRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.eventRepository = eventRepository;
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.fingerprintService = fingerprintService;
        this.entityManager = entityManager;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public GroupingOutcome groupErrorEvent(Long projectId, Long sessionPk, Long eventPk, IngestEventDto dto) {
        setMySqlActor("TRACKER");
        try {
            Event event = eventRepository.findById(eventPk)
                    .orElseThrow(() -> new IllegalStateException("Event not found: " + eventPk));
            Project project = projectRepository.getReferenceById(projectId);
            TrackingSession session = sessionRepository.getReferenceById(sessionPk);

            ErrorType errorType = toErrorType(event.getType());

            // 1. Deterministic SHA-256 fingerprint (AI never groups bugs)
            String canonical = fingerprintService.computeCanonical(
                    errorType,
                    event.getHttpMethod(),
                    event.getEndpoint(),
                    event.getHttpStatus() != null ? event.getHttpStatus().intValue() : null,
                    event.getErrorCode(),
                    dto.errorName(),
                    event.getMessage(),
                    event.getStackTop()
            );
            String fingerprint = fingerprintService.computeFingerprint(canonical);

            LocalDateTime occurredAt = event.getOccurredAt() != null ? event.getOccurredAt() : LocalDateTime.now();

            // 2. Lock existing bug row (SELECT ... FOR UPDATE) to serialize concurrent occurrence bumps
            Optional<Bug> existingOpt = bugRepository.findForUpdateByProjectIdAndFingerprint(projectId, fingerprint);

            Bug bug;
            boolean created = false;
            boolean updated = false;
            boolean wasRegression = false;

            if (existingOpt.isEmpty()) {
                bug = new Bug();
                bug.setProject(project);
                bug.setFingerprint(fingerprint);
                bug.setTitle(buildTitle(errorType, event, dto));
                bug.setErrorType(errorType);
                bug.setErrorCode(event.getErrorCode());
                bug.setHttpMethod(event.getHttpMethod());
                bug.setEndpoint(event.getEndpoint());
                bug.setPagePath(event.getPagePath());
                bug.setStatus(BugStatus.OPEN);
                bug.setPriority(deriveInitialPriority(errorType, event));
                bug.setOccurrenceCount(1);
                bug.setRegression(false);
                bug.setRegressionCount(0);
                bug.setFirstSeenAt(occurredAt);
                bug.setLastSeenAt(occurredAt);

                // Flush immediately so UNIQUE(project_id, fingerprint) race surfaces right here
                bug = bugRepository.saveAndFlush(bug);
                created = true;
            } else {
                bug = existingOpt.get();
                bug.setOccurrenceCount(bug.getOccurrenceCount() + 1);
                if (bug.getLastSeenAt() == null || occurredAt.isAfter(bug.getLastSeenAt())) {
                    bug.setLastSeenAt(occurredAt);
                }

                // Regression detection state machine:
                // RESOLVED or CLOSED bugs that recur auto-reopen and flag regression;
                // IGNORED bugs stay IGNORED while still tracking occurrence_count.
                if (bug.getStatus() == BugStatus.RESOLVED || bug.getStatus() == BugStatus.CLOSED) {
                    bug.setStatus(BugStatus.REOPENED);
                    bug.setRegression(true);
                    bug.setRegressionCount(bug.getRegressionCount() + 1);
                    bug.setResolvedAt(null);
                    wasRegression = true;
                }

                bug = bugRepository.saveAndFlush(bug);
                updated = true;
            }

            // 3. Record occurrence row and link event.bug_id
            BugOccurrence occurrence = new BugOccurrence(bug, session, event, occurredAt, wasRegression);
            occurrenceRepository.saveAndFlush(occurrence);

            event.setBug(bug);
            eventRepository.saveAndFlush(event);

            return new GroupingOutcome(bug.getId(), created, updated, wasRegression);
        } finally {
            setMySqlActor("API");
        }
    }

    private ErrorType toErrorType(EventType type) {
        return switch (type) {
            case API_ERROR -> ErrorType.API_ERROR;
            case JS_ERROR -> ErrorType.JS_ERROR;
            case PROMISE_REJECTION -> ErrorType.PROMISE_REJECTION;
            case NETWORK_ERROR -> ErrorType.NETWORK_ERROR;
            default -> throw new IllegalArgumentException("Not an error event type: " + type);
        };
    }

    private String buildTitle(ErrorType type, Event event, IngestEventDto dto) {
        String raw;
        if (type == ErrorType.API_ERROR) {
            String method = event.getHttpMethod() != null ? event.getHttpMethod() : "HTTP";
            String ep = event.getEndpoint() != null ? event.getEndpoint() : "/";
            String status = event.getHttpStatus() != null ? String.valueOf(event.getHttpStatus()) : "ERR";
            String code = (event.getErrorCode() != null && !event.getErrorCode().isBlank())
                    ? " " + event.getErrorCode()
                    : "";
            raw = method + " " + ep + " → " + status + code;
        } else if (type == ErrorType.NETWORK_ERROR) {
            String method = event.getHttpMethod() != null ? event.getHttpMethod() : "FETCH";
            String ep = event.getEndpoint() != null ? event.getEndpoint() : "/";
            raw = "Failed to fetch " + method + " " + ep;
        } else {
            String errName = (dto.errorName() != null && !dto.errorName().isBlank())
                    ? dto.errorName().trim()
                    : (type == ErrorType.PROMISE_REJECTION ? "UnhandledRejection" : "Error");
            String msg = (event.getMessage() != null && !event.getMessage().isBlank())
                    ? event.getMessage()
                    : "Uncaught client error";
            raw = msg.startsWith(errName) ? msg : errName + ": " + msg;
        }
        return raw.length() > 200 ? raw.substring(0, 200) : raw;
    }

    private BugPriority deriveInitialPriority(ErrorType type, Event event) {
        if (type == ErrorType.API_ERROR && event.getHttpStatus() != null && event.getHttpStatus() >= 500) {
            return BugPriority.HIGH;
        }
        if (type == ErrorType.JS_ERROR) {
            return BugPriority.HIGH;
        }
        return BugPriority.MEDIUM;
    }

    private void setMySqlActor(String actor) {
        try {
            entityManager.createNativeQuery("SET @bugdna_actor = :actor")
                    .setParameter("actor", actor)
                    .executeUpdate();
        } catch (Exception ignored) {
            // Gracefully ignore on non-MySQL dialects (e.g., H2 tests)
        }
    }
}

