package com.bugdna.ingest;

import com.bugdna.common.UnauthorizedException;
import com.bugdna.event.Event;
import com.bugdna.event.EventRepository;
import com.bugdna.event.EventType;
import com.bugdna.event.TrackingSession;
import com.bugdna.event.TrackingSessionRepository;
import com.bugdna.ingest.dto.IngestBatchRequest;
import com.bugdna.ingest.dto.IngestBatchResponse;
import com.bugdna.ingest.dto.IngestEventDto;
import com.bugdna.project.Project;
import com.bugdna.project.ProjectRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class IngestService {

    private static final Set<EventType> ERROR_EVENT_TYPES = EnumSet.of(
            EventType.JS_ERROR,
            EventType.PROMISE_REJECTION,
            EventType.API_ERROR,
            EventType.NETWORK_ERROR
    );

    private final ProjectRepository projectRepository;
    private final TrackingSessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final Sanitizer sanitizer;
    private final BugGroupingService bugGroupingService;
    private final TransactionTemplate requiresNewTx;

    public IngestService(
            ProjectRepository projectRepository,
            TrackingSessionRepository sessionRepository,
            EventRepository eventRepository,
            Sanitizer sanitizer,
            BugGroupingService bugGroupingService,
            PlatformTransactionManager transactionManager
    ) {
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.sanitizer = sanitizer;
        this.bugGroupingService = bugGroupingService;
        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public IngestBatchResponse ingestBatch(String projectKey, IngestBatchRequest request) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new UnauthorizedException("Missing X-BugDNA-Key header");
        }

        Project project = projectRepository.findByProjectKey(projectKey.trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid project key"));

        // 1. Upsert tracking session (retry once if two concurrent batches create the same session)
        Long sessionPk = upsertSessionWithRetry(project.getId(), request.sessionId(), request.userAgent());

        int accepted = 0;
        int duplicates = 0;
        Set<Long> bugsCreated = new LinkedHashSet<>();
        Set<Long> bugsUpdated = new LinkedHashSet<>();
        Set<Long> regressions = new LinkedHashSet<>();

        // 2. Persist each event idempotently, then group error events deterministically
        for (IngestEventDto dto : request.events()) {
            Long savedEventPk = saveEventIdempotently(project.getId(), sessionPk, dto);
            if (savedEventPk == null) {
                duplicates++;
                continue;
            }

            accepted++;

            if (ERROR_EVENT_TYPES.contains(dto.type())) {
                BugGroupingService.GroupingOutcome outcome = groupErrorWithRetry(project.getId(), sessionPk, savedEventPk, dto);
                if (outcome.created()) {
                    bugsCreated.add(outcome.bugId());
                } else if (outcome.updated()) {
                    bugsUpdated.add(outcome.bugId());
                }
                if (outcome.regression()) {
                    regressions.add(outcome.bugId());
                }
            }
        }

        return new IngestBatchResponse(
                accepted,
                duplicates,
                new ArrayList<>(bugsCreated),
                new ArrayList<>(bugsUpdated),
                new ArrayList<>(regressions)
        );
    }

    private Long upsertSessionWithRetry(Long projectId, String sessionId, String rawUserAgent) {
        String cleanUa = sanitizer.sanitizeText(rawUserAgent, 255);
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return requiresNewTx.execute(status -> {
                    LocalDateTime now = LocalDateTime.now();
                    TrackingSession session = sessionRepository
                            .findByProjectIdAndSessionId(projectId, sessionId)
                            .map(existing -> {
                                existing.setLastSeenAt(now);
                                if (cleanUa != null && existing.getUserAgent() == null) {
                                    existing.setUserAgent(cleanUa);
                                }
                                return sessionRepository.saveAndFlush(existing);
                            })
                            .orElseGet(() -> {
                                Project projectRef = projectRepository.getReferenceById(projectId);
                                TrackingSession created = new TrackingSession(projectRef, sessionId, cleanUa, now, now);
                                return sessionRepository.saveAndFlush(created);
                            });
                    return session.getId();
                });
            } catch (DataIntegrityViolationException ex) {
                if (attempt == 2) throw ex;
            }
        }
        throw new IllegalStateException("Failed to upsert tracking session: " + sessionId);
    }

    private Long saveEventIdempotently(Long projectId, Long sessionPk, IngestEventDto dto) {
        try {
            return requiresNewTx.execute(status -> {
                if (eventRepository.findByProjectIdAndEventId(projectId, dto.eventId()).isPresent()) {
                    return null;
                }

                Project projectRef = projectRepository.getReferenceById(projectId);
                TrackingSession sessionRef = sessionRepository.getReferenceById(sessionPk);

                Event event = new Event();
                event.setProject(projectRef);
                event.setSession(sessionRef);
                event.setEventId(dto.eventId());
                event.setType(dto.type());
                event.setActionLabel(sanitizer.sanitizeText(dto.actionLabel(), 80));
                event.setPagePath(sanitizer.sanitizePath(dto.pagePath()));
                event.setHttpMethod(dto.httpMethod() != null ? dto.httpMethod().toUpperCase().trim() : null);
                event.setHttpStatus(dto.httpStatus() != null ? dto.httpStatus().shortValue() : null);
                event.setEndpoint(dto.endpoint() != null ? sanitizer.sanitizePath(dto.endpoint()) : null);
                event.setErrorCode(sanitizer.sanitizeText(dto.errorCode(), 80));
                event.setMessage(sanitizer.sanitizeText(dto.message(), 500));
                event.setStackTop(sanitizer.sanitizeText(dto.stackTop(), 500));

                // Convert browser ISO instant to server local time to match MySQL NOW(3) and LocalDateTime.now()
                LocalDateTime occurredAt = dto.occurredAt() != null
                        ? LocalDateTime.ofInstant(dto.occurredAt(), ZoneId.systemDefault())
                        : LocalDateTime.now();
                event.setOccurredAt(occurredAt);

                Event saved = eventRepository.saveAndFlush(event);
                return saved.getId();
            });
        } catch (DataIntegrityViolationException ex) {
            // Concurrent duplicate (project_id, event_id) caught safely
            return null;
        }
    }

    private BugGroupingService.GroupingOutcome groupErrorWithRetry(
            Long projectId, Long sessionPk, Long eventPk, IngestEventDto dto
    ) {
        int maxAttempts = 6;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return bugGroupingService.groupErrorEvent(projectId, sessionPk, eventPk, dto);
            } catch (DataIntegrityViolationException | PessimisticLockingFailureException ex) {
                // Concurrent INSERT on UNIQUE(project_id, fingerprint) or lock timeout:
                // retry cleanly so this occurrence increments the winning bug row.
                if (attempt == maxAttempts) {
                    throw ex;
                }
                try {
                    Thread.sleep(15L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw new IllegalStateException("Exhausted grouping retries for event " + eventPk);
    }
}
