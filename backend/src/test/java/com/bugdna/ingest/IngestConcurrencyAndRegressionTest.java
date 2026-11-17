package com.bugdna.ingest;

import com.bugdna.bug.*;
import com.bugdna.event.EventType;
import com.bugdna.event.TimelineService;
import com.bugdna.event.dto.TimelineResponseDto;
import com.bugdna.ingest.dto.IngestBatchRequest;
import com.bugdna.ingest.dto.IngestBatchResponse;
import com.bugdna.ingest.dto.IngestEventDto;
import com.bugdna.project.Project;
import com.bugdna.project.ProjectRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IngestConcurrencyAndRegressionTest {

    @Autowired
    private IngestService ingestService;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private BugRepository bugRepository;

    @Autowired
    private BugOccurrenceRepository occurrenceRepository;

    @Autowired
    private TimelineService timelineService;

    private Project project;

    @BeforeEach
    void setUp() {
        project = projectRepository.findByProjectKey("concurrency-test-key")
                .orElseGet(() -> projectRepository.save(
                        new Project("Concurrency Test Project", "concurrency-test-key", "http://localhost:5174")));
    }

    @Test
    @DisplayName("P4-7: 20 parallel ingests of the exact same new error produce exactly 1 bug with occurrence_count = 20")
    void testTwentyParallelIngestsProduceSingleBug() throws Exception {
        int threadCount = 20;
        String uniqueCode = "CONCURRENT_ERR_" + UUID.randomUUID().toString().substring(0, 8);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<IngestBatchResponse>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                IngestEventDto event = new IngestEventDto(
                        "conc-evt-" + uniqueCode + "-" + idx,
                        EventType.API_ERROR,
                        null,
                        "/checkout",
                        "POST",
                        500,
                        "/demo-api/checkout",
                        uniqueCode,
                        null,
                        "Coupon rule evaluation failed",
                        null,
                        Instant.now()
                );
                IngestBatchRequest req = new IngestBatchRequest(
                        "conc-sess-" + idx,
                        "JUnit-Concurrency-Agent",
                        List.of(event)
                );
                return ingestService.ingestBatch("concurrency-test-key", req);
            }));
        }

        assertTrue(readyLatch.await(10, TimeUnit.SECONDS));
        startLatch.countDown();

        int totalAccepted = 0;
        int totalCreated = 0;
        Long bugId = null;

        for (Future<IngestBatchResponse> f : futures) {
            IngestBatchResponse res = f.get(30, TimeUnit.SECONDS);
            totalAccepted += res.accepted();
            totalCreated += res.bugsCreated().size();
            if (!res.bugsCreated().isEmpty()) {
                bugId = res.bugsCreated().get(0);
            } else if (!res.bugsUpdated().isEmpty() && bugId == null) {
                bugId = res.bugsUpdated().get(0);
            }
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(threadCount, totalAccepted, "All 20 distinct events should be accepted");
        assertEquals(1, totalCreated, "Exactly 1 bug should be created across 20 racing threads");
        assertNotNull(bugId);

        Bug bug = bugRepository.findById(bugId).orElseThrow();
        assertEquals(threadCount, bug.getOccurrenceCount(), "occurrence_count must equal 20");
        assertEquals(threadCount, occurrenceRepository.findByBugIdOrderByOccurredAtDesc(bugId).size());
    }

    @Test
    @DisplayName("Regression detection: RESOLVED bug auto-reopens with REGRESSION flag; IGNORED bug stays IGNORED")
    void testRegressionDetectionAndIgnoredBehavior() {
        String code = "REG_ERR_" + UUID.randomUUID().toString().substring(0, 8);
        IngestEventDto first = new IngestEventDto(
                "reg-1-" + code, EventType.API_ERROR, null, "/checkout",
                "POST", 500, "/demo-api/checkout", code, null, "Failure", null, Instant.now()
        );
        IngestBatchResponse r1 = ingestService.ingestBatch("concurrency-test-key",
                new IngestBatchRequest("reg-sess-1", "JUnit", List.of(first)));

        assertEquals(1, r1.bugsCreated().size());
        Long bugId = r1.bugsCreated().get(0);

        // Mark bug RESOLVED
        Bug bug = bugRepository.findById(bugId).orElseThrow();
        bug.setStatus(BugStatus.RESOLVED);
        bug.setResolvedAt(LocalDateTime.now());
        bugRepository.saveAndFlush(bug);

        // Trigger the exact same error again -> must auto-reopen as REGRESSION
        IngestEventDto second = new IngestEventDto(
                "reg-2-" + code, EventType.API_ERROR, null, "/checkout",
                "POST", 500, "/demo-api/checkout", code, null, "Failure", null, Instant.now()
        );
        IngestBatchResponse r2 = ingestService.ingestBatch("concurrency-test-key",
                new IngestBatchRequest("reg-sess-2", "JUnit", List.of(second)));

        assertTrue(r2.regressions().contains(bugId), "Regression bugId should be returned");
        Bug reopened = bugRepository.findById(bugId).orElseThrow();
        assertEquals(BugStatus.REOPENED, reopened.getStatus());
        assertTrue(reopened.isRegression());
        assertEquals(1, reopened.getRegressionCount());
        assertNull(reopened.getResolvedAt());
        assertEquals(2, reopened.getOccurrenceCount());

        // Now mark bug IGNORED -> recurring error increments count but does NOT reopen
        reopened.setStatus(BugStatus.IGNORED);
        bugRepository.saveAndFlush(reopened);

        IngestEventDto third = new IngestEventDto(
                "reg-3-" + code, EventType.API_ERROR, null, "/checkout",
                "POST", 500, "/demo-api/checkout", code, null, "Failure", null, Instant.now()
        );
        IngestBatchResponse r3 = ingestService.ingestBatch("concurrency-test-key",
                new IngestBatchRequest("reg-sess-3", "JUnit", List.of(third)));

        assertTrue(r3.regressions().isEmpty(), "IGNORED bug should not flag a new regression");
        Bug stillIgnored = bugRepository.findById(bugId).orElseThrow();
        assertEquals(BugStatus.IGNORED, stillIgnored.getStatus());
        assertEquals(3, stillIgnored.getOccurrenceCount());
    }

    @Test
    @DisplayName("Timeline returns click-to-crash events in chronological order with negative relative offsets")
    void testClickToCrashTimelineOrder() {
        String uid = UUID.randomUUID().toString().substring(0, 8);
        String sessId = "tl-sess-" + uid;
        Instant base = Instant.now().minusSeconds(10);

        List<IngestEventDto> flow = List.of(
                new IngestEventDto("tl-1-" + uid, EventType.PAGE_VIEW, null, "/cart", null, null, null, null, null, null, null, base),
                new IngestEventDto("tl-2-" + uid, EventType.CLICK, "APPLY_COUPON", "/cart", null, null, null, null, null, null, null, base.plusSeconds(2)),
                new IngestEventDto("tl-3-" + uid, EventType.ROUTE_CHANGE, null, "/checkout", null, null, null, null, null, null, null, base.plusSeconds(5)),
                new IngestEventDto("tl-4-" + uid, EventType.CLICK, "CHECKOUT", "/checkout", null, null, null, null, null, null, null, base.plusSeconds(8)),
                new IngestEventDto("tl-5-" + uid, EventType.API_ERROR, null, "/checkout", "POST", 500, "/demo-api/checkout", "TL_ERR_" + uid, null, "Coupon rule failed", null, base.plusSeconds(9))
        );

        IngestBatchResponse res = ingestService.ingestBatch("concurrency-test-key",
                new IngestBatchRequest(sessId, "JUnit", flow));

        assertEquals(5, res.accepted());
        assertEquals(1, res.bugsCreated().size());
        Long bugId = res.bugsCreated().get(0);

        TimelineResponseDto timeline = timelineService.getTimeline(bugId, null);
        assertEquals(5, timeline.events().size());
        assertEquals(EventType.PAGE_VIEW, timeline.events().get(0).type());
        assertEquals("APPLY_COUPON", timeline.events().get(1).actionLabel());
        assertEquals(EventType.ROUTE_CHANGE, timeline.events().get(2).type());
        assertEquals("CHECKOUT", timeline.events().get(3).actionLabel());
        assertEquals(EventType.API_ERROR, timeline.events().get(4).type());
        assertTrue(timeline.events().get(4).isTrigger());
        assertTrue(timeline.events().get(0).offsetMs() < 0, "Pre-crash events must have negative offsetMs");
        assertEquals(0L, timeline.events().get(4).offsetMs());
    }
}

