package com.bugdna.ingest;

import com.bugdna.common.ApiExceptionHandler;
import com.bugdna.common.UnauthorizedException;
import com.bugdna.ingest.dto.IngestBatchRequest;
import com.bugdna.ingest.dto.IngestBatchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IngestControllerTest {

    private MockMvc mockMvc;
    private IngestService ingestService;

    @BeforeEach
    void setUp() {
        ingestService = mock(IngestService.class);
        IngestController controller = new IngestController(ingestService);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/ingest/events with wrong key returns 401 ProblemDetail")
    void testWrongProjectKeyReturns401() throws Exception {
        when(ingestService.ingestBatch(eq("bad-key"), any(IngestBatchRequest.class)))
                .thenThrow(new UnauthorizedException("Invalid project key"));

        String payload = """
            {
              "sessionId": "sess-123",
              "events": [
                {
                  "eventId": "evt-1",
                  "type": "PAGE_VIEW",
                  "pagePath": "/cart"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/ingest/events")
                        .header("X-BugDNA-Key", "bad-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    @DisplayName("POST /api/v1/ingest/events with valid key returns 202 Accepted")
    void testValidBatchReturns202() throws Exception {
        when(ingestService.ingestBatch(eq("demo-store-dev-key"), any(IngestBatchRequest.class)))
                .thenReturn(new IngestBatchResponse(1, 0, List.of(), List.of(), List.of()));

        String payload = """
            {
              "sessionId": "sess-123",
              "events": [
                {
                  "eventId": "evt-1",
                  "type": "CLICK",
                  "actionLabel": "APPLY_COUPON",
                  "pagePath": "/cart"
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/ingest/events")
                        .header("X-BugDNA-Key", "demo-store-dev-key")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted").value(1))
                .andExpect(jsonPath("$.duplicates").value(0));
    }
}

