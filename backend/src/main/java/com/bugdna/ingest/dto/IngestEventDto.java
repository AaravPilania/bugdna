package com.bugdna.ingest.dto;

import com.bugdna.event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record IngestEventDto(
    @NotBlank(message = "eventId is required")
    String eventId,

    @NotNull(message = "event type is required")
    EventType type,

    String actionLabel,
    String pagePath,
    String httpMethod,
    Integer httpStatus,
    String endpoint,
    String errorCode,
    String errorName,
    String message,
    String stackTop,
    Instant occurredAt
) {}

