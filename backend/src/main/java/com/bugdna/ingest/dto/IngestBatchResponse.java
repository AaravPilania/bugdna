package com.bugdna.ingest.dto;

import java.util.List;

public record IngestBatchResponse(
    int accepted,
    int duplicates,
    List<Long> bugsCreated,
    List<Long> bugsUpdated,
    List<Long> regressions
) {}

