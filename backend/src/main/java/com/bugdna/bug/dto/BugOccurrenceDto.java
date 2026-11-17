package com.bugdna.bug.dto;

import com.bugdna.bug.BugOccurrence;

import java.time.LocalDateTime;

public record BugOccurrenceDto(
    Long id,
    Long bugId,
    String sessionId,
    Long triggerEventId,
    String triggerEventUuid,
    LocalDateTime occurredAt,
    boolean wasRegression
) {
    public static BugOccurrenceDto fromEntity(BugOccurrence occ) {
        return new BugOccurrenceDto(
            occ.getId(),
            occ.getBug() != null ? occ.getBug().getId() : null,
            occ.getSession() != null ? occ.getSession().getSessionId() : null,
            occ.getTriggerEvent() != null ? occ.getTriggerEvent().getId() : null,
            occ.getTriggerEvent() != null ? occ.getTriggerEvent().getEventId() : null,
            occ.getOccurredAt(),
            occ.isWasRegression()
        );
    }
}
