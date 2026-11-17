package com.bugdna.ingest;

import com.bugdna.common.UnauthorizedException;
import com.bugdna.event.Event;
import com.bugdna.event.EventRepository;
import com.bugdna.event.TrackingSession;
import com.bugdna.event.TrackingSessionRepository;
import com.bugdna.ingest.dto.IngestBatchRequest;
import com.bugdna.ingest.dto.IngestBatchResponse;
import com.bugdna.ingest.dto.IngestEventDto;
import com.bugdna.project.Project;
import com.bugdna.project.ProjectRepository;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class IngestService {

    private final ProjectRepository projectRepository;
    private final TrackingSessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final Sanitizer sanitizer;
    private final EntityManager entityManager;

    public IngestService(
            ProjectRepository projectRepository,
            TrackingSessionRepository sessionRepository,
            EventRepository eventRepository,
            Sanitizer sanitizer,
            EntityManager entityManager
    ) {
        this.projectRepository = projectRepository;
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.sanitizer = sanitizer;
        this.entityManager = entityManager;
    }

    @Transactional
    public IngestBatchResponse ingestBatch(String projectKey, IngestBatchRequest request) {
        if (projectKey == null || projectKey.isBlank()) {
            throw new UnauthorizedException("Missing X-BugDNA-Key header");
        }

        Project project = projectRepository.findByProjectKey(projectKey.trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid project key"));

        // Mark actor as TRACKER so MySQL triggers attribute events to TRACKER
        setMySqlActor("TRACKER");
        try {
            LocalDateTime now = LocalDateTime.now();

            // 1. Upsert tracking session
            TrackingSession session = sessionRepository
                    .findByProjectIdAndSessionId(project.getId(), request.sessionId())
                    .map(existing -> {
                        existing.setLastSeenAt(now);
                        if (request.userAgent() != null && existing.getUserAgent() == null) {
                            existing.setUserAgent(sanitizer.sanitizeText(request.userAgent(), 255));
                        }
                        return sessionRepository.save(existing);
                    })
                    .orElseGet(() -> {
                        TrackingSession created = new TrackingSession(
                                project,
                                request.sessionId(),
                                sanitizer.sanitizeText(request.userAgent(), 255),
                                now,
                                now
                        );
                        return sessionRepository.save(created);
                    });

            int accepted = 0;
            int duplicates = 0;
            List<Long> bugsCreated = new ArrayList<>();
            List<Long> bugsUpdated = new ArrayList<>();
            List<Long> regressions = new ArrayList<>();

            // 2. Process events with server-side sanitization and idempotent dedupe
            for (IngestEventDto dto : request.events()) {
                // Fast check for duplicate eventId
                if (eventRepository.findByProjectIdAndEventId(project.getId(), dto.eventId()).isPresent()) {
                    duplicates++;
                    continue;
                }

                Event event = new Event();
                event.setProject(project);
                event.setSession(session);
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

                LocalDateTime occurredAt = dto.occurredAt() != null
                        ? LocalDateTime.ofInstant(dto.occurredAt(), ZoneOffset.UTC)
                        : now;
                event.setOccurredAt(occurredAt);

                try {
                    eventRepository.saveAndFlush(event);
                    accepted++;
                } catch (DataIntegrityViolationException ex) {
                    // Concurrent duplicate eventId race caught safely
                    duplicates++;
                }
            }

            return new IngestBatchResponse(accepted, duplicates, bugsCreated, bugsUpdated, regressions);
        } finally {
            // Restore pooled connection actor back to default API
            setMySqlActor("API");
        }
    }

    private void setMySqlActor(String actor) {
        try {
            entityManager.createNativeQuery("SET @bugdna_actor = :actor")
                    .setParameter("actor", actor)
                    .executeUpdate();
        } catch (Exception ignored) {
            // Gracefully ignore when running on non-MySQL dialects (e.g., H2 unit tests)
        }
    }
}

