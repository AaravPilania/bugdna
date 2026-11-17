package com.bugdna.event.dto;

import com.bugdna.event.Event;
import com.bugdna.event.EventType;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record TimelineEventDto(
    Long id,
    String eventId,
    EventType type,
    String actionLabel,
    String pagePath,
    String httpMethod,
    Short httpStatus,
    String endpoint,
    String errorCode,
    String message,
    String stackTop,
    LocalDateTime occurredAt,
    long offsetMs,
    boolean isTrigger
) {
    public static TimelineEventDto fromEntity(Event event, LocalDateTime triggerTime, Long triggerEventPk) {
        long offsetMs = (triggerTime != null && event.getOccurredAt() != null)
                ? ChronoUnit.MILLIS.between(triggerTime, event.getOccurredAt())
                : 0L;
        boolean isTrigger = triggerEventPk != null && triggerEventPk.equals(event.getId());

        return new TimelineEventDto(
                event.getId(),
                event.getEventId(),
                event.getType(),
                event.getActionLabel(),
                event.getPagePath(),
                event.getHttpMethod(),
                event.getHttpStatus(),
                event.getEndpoint(),
                event.getErrorCode(),
                event.getMessage(),
                event.getStackTop(),
                event.getOccurredAt(),
                offsetMs,
                isTrigger
        );
    }
}
