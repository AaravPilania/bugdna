package com.bugdna.ingest;

import com.bugdna.ingest.dto.IngestBatchRequest;
import com.bugdna.ingest.dto.IngestBatchResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingest")
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/events")
    public ResponseEntity<IngestBatchResponse> ingestEvents(
            @RequestHeader(value = "X-BugDNA-Key", required = false) String projectKey,
            @Valid @RequestBody IngestBatchRequest request
    ) {
        IngestBatchResponse response = ingestService.ingestBatch(projectKey, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

