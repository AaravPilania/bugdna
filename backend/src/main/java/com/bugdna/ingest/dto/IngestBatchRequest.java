package com.bugdna.ingest.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record IngestBatchRequest(
    @NotBlank(message = "sessionId is required")
    @Size(max = 64)
    String sessionId,

    @Size(max = 255)
    String userAgent,

    @NotEmpty(message = "events batch must not be empty")
    @Size(max = 50, message = "Maximum 50 events per batch")
    List<@Valid IngestEventDto> events
) {}
