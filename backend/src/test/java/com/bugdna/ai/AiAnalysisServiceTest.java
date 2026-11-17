package com.bugdna.ai;

import com.bugdna.ai.AiAnalysisDtos.*;
import com.bugdna.analytics.BugSummaryViewRepository;
import com.bugdna.bug.Bug;
import com.bugdna.bug.BugRepository;
import com.bugdna.bug.ErrorType;
import com.bugdna.event.EventType;
import com.bugdna.event.TimelineService;
import com.bugdna.event.dto.TimelineEventDto;
import com.bugdna.event.dto.TimelineResponseDto;
import com.bugdna.ingest.FingerprintService;
import com.bugdna.ingest.Sanitizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AiAnalysisServiceTest {

    private BugRepository bugRepository;
    private BugSummaryViewRepository summaryRepository;
    private TimelineService timelineService;
    private AiAnalysisRepository aiAnalysisRepository;
    private MockAiProvider mockAiProvider;
    private AiAnalysisService service;

    @BeforeEach
    void setUp() {
        bugRepository = mock(BugRepository.class);
        summaryRepository = mock(BugSummaryViewRepository.class);
        timelineService = mock(TimelineService.class);
        aiAnalysisRepository = mock(AiAnalysisRepository.class);
        FingerprintService fingerprintService = new FingerprintService();
        Sanitizer sanitizer = new Sanitizer();
        ObjectMapper objectMapper = new ObjectMapper();
        GeminiProvider geminiProvider = new GeminiProvider(objectMapper);
        OpenAiProvider openAiProvider = new OpenAiProvider(objectMapper);
        mockAiProvider = spy(new MockAiProvider());

        service = new AiAnalysisService(
                bugRepository,
                summaryRepository,
                timelineService,
                aiAnalysisRepository,
                fingerprintService,
                sanitizer,
                geminiProvider,
                openAiProvider,
                mockAiProvider,
                objectMapper
        );
    }

    @Test
    @DisplayName("P5-4: Prompt JSON contains NO email, token, digit run, sessionId, or userAgent even when stored events contained them")
    void testPromptInputContainsZeroPiiOrSessionIdentifiers() {
        Bug bug = new Bug();
        bug.setId(200L);
        bug.setTitle("POST /demo-api/checkout -> 500 for leak.user@example.com");
        bug.setErrorType(ErrorType.API_ERROR);
        bug.setHttpMethod("POST");
        bug.setEndpoint("/demo-api/checkout?token=secretQuery123");
        bug.setErrorCode("DEMO_COUPON_RULE_ERROR");
        bug.setOccurrenceCount(3);

        TimelineEventDto dirtyEvent = new TimelineEventDto(
                10L,
                "evt-real-1",
                EventType.API_ERROR,
                null,
                "/checkout?email=leak.user@example.com",
                "POST",
                (short) 500,
                "/demo-api/checkout?auth=xyz",
                "DEMO_COUPON_RULE_ERROR",
                "Failed for leak.user@example.com card 4111222233334444 Bearer eyJhbGciOiJIUzI1NiJ9.supersecret",
                null,
                LocalDateTime.now(),
                0L,
                true
        );

        TimelineResponseDto timeline = new TimelineResponseDto(
                200L,
                50L,
                "sess-private-identifier-999",
                LocalDateTime.now(),
                false,
                List.of(dirtyEvent)
        );

        SanitizedBugInput input = service.buildSanitizedInput(bug, null, timeline);
        String promptJson = service.serializePromptInput(input);

        assertFalse(promptJson.contains("leak.user@example.com"), "Prompt must not leak email");
        assertFalse(promptJson.contains("4111222233334444"), "Prompt must not leak long digit run");
        assertFalse(promptJson.contains("eyJhbGciOiJIUzI1NiJ9"), "Prompt must not leak Bearer token");
        assertFalse(promptJson.contains("sess-private-identifier-999"), "Prompt must not include sessionId");
        assertFalse(promptJson.contains("secretQuery123"), "Prompt must not include query string");
        assertFalse(promptJson.contains("userAgent"), "Prompt must not include userAgent field");
        assertTrue(promptJson.contains("[email]"));
        assertTrue(promptJson.contains("Bearer [token]"));
    }

    @Test
    @DisplayName("Output validator drops hallucinated eventIds and hypotheses without reasoning")
    void testOutputValidationDropsHallucinations() {
        AiStructuredOutput raw = new AiStructuredOutput(
                "Summary text",
                List.of(
                        new EvidenceItem("Real fact citing recorded event", List.of("evt-real-1", "evt-fake-999")),
                        new EvidenceItem("Completely fabricated event citation", List.of("evt-hallucinated-404"))
                ),
                List.of(
                        new HypothesisItem("Coupon rule exception", "high", "Observed 500 immediately after APPLY_COUPON"),
                        new HypothesisItem("Unsupported guess", "medium", "   ")
                ),
                List.of("Check checkout controller"),
                List.of("Apply SAVE20 and click Checkout")
        );

        AiStructuredOutput validated = service.validateOutput(raw, Set.of("evt-real-1"));

        assertEquals(1, validated.evidence().size(), "Hallucinated evidence item must be dropped");
        assertEquals(List.of("evt-real-1"), validated.evidence().get(0).eventIds(), "Only real eventId must remain");

        assertEquals(1, validated.hypotheses().size(), "Hypothesis with blank reasoning must be dropped");
        assertEquals("high", validated.hypotheses().get(0).confidence());
    }

    @Test
    @DisplayName("P5-3: Second analyze call returns cached row without calling provider; force=true reruns provider")
    void testCachingAndForceRerun() {
        Bug bug = new Bug();
        bug.setId(200L);
        bug.setTitle("POST /demo-api/checkout -> 500 DEMO_COUPON_RULE_ERROR");
        bug.setErrorType(ErrorType.API_ERROR);
        bug.setHttpMethod("POST");
        bug.setEndpoint("/demo-api/checkout");
        bug.setErrorCode("DEMO_COUPON_RULE_ERROR");
        bug.setOccurrenceCount(1);

        TimelineEventDto ev = new TimelineEventDto(
                1L, "evt-1", EventType.API_ERROR, null, "/checkout",
                "POST", (short) 500, "/demo-api/checkout", "DEMO_COUPON_RULE_ERROR",
                "Coupon rule evaluation failed", null, LocalDateTime.now(), 0L, true
        );
        TimelineResponseDto timeline = new TimelineResponseDto(200L, 1L, "sess-1", LocalDateTime.now(), false, List.of(ev));

        when(bugRepository.findById(200L)).thenReturn(Optional.of(bug));
        when(summaryRepository.findById(200L)).thenReturn(Optional.empty());
        when(timelineService.getTimeline(200L, null)).thenReturn(timeline);

        // First call: cache miss -> saves and stores entity
        final AiAnalysis[] stored = new AiAnalysis[1];
        when(aiAnalysisRepository.findByBugIdAndInputHash(eq(200L), anyString()))
                .thenAnswer(inv -> Optional.ofNullable(stored[0]));
        when(aiAnalysisRepository.saveAndFlush(any(AiAnalysis.class)))
                .thenAnswer(inv -> {
                    AiAnalysis a = inv.getArgument(0);
                    a.setId(99L);
                    stored[0] = a;
                    return a;
                });

        AiAnalysisResponse first = service.analyzeBug(200L, false);
        assertFalse(first.cached());
        assertEquals(AiStatus.MOCK, first.status());
        verify(mockAiProvider, times(1)).analyze(any(), anyString(), anyString());

        // Second call without force: cache hit -> provider NOT called again!
        AiAnalysisResponse second = service.analyzeBug(200L, false);
        assertTrue(second.cached());
        assertEquals(99L, second.id());
        verify(mockAiProvider, times(1)).analyze(any(), anyString(), anyString());

        // Third call with force=true: bypasses cache and calls provider again!
        AiAnalysisResponse forced = service.analyzeBug(200L, true);
        assertFalse(forced.cached());
        verify(mockAiProvider, times(2)).analyze(any(), anyString(), anyString());
    }
}
