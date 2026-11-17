package com.bugdna.event.dto;

import java.time.LocalDateTime;
import java.util.List;

public record TimelineResponseDto(
    Long bugId,
    Long occurrenceId,
    String sessionId,
    LocalDateTime triggerOccurredAt,
    boolean wasRegression,
    List<TimelineEventDto> events
) {}

