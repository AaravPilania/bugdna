package com.bugdna.analytics;

import com.bugdna.bug.BugPriority;
import com.bugdna.bug.BugStatus;
import com.bugdna.event.EventRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsControllerTest {

    @Test
    @DisplayName("GET /api/v1/analytics/overview aggregates bug_summary counts, regressions, mean resolve time, and 14-day series")
    void testAnalyticsOverviewAggregation() {
        BugSummaryViewRepository viewRepo = mock(BugSummaryViewRepository.class);
        EventRepository eventRepo = mock(EventRepository.class);

        BugSummaryView b1 = mock(BugSummaryView.class);
        when(b1.getId()).thenReturn(101L);
        when(b1.getTitle()).thenReturn("TypeError");
        when(b1.getStatus()).thenReturn(BugStatus.OPEN);
        when(b1.getPriority()).thenReturn(BugPriority.HIGH);
        when(b1.getOccurrenceCount()).thenReturn(5);
        when(b1.getSessionsAffected()).thenReturn(2L);
        when(b1.isRegression()).thenReturn(false);

        BugSummaryView b2 = mock(BugSummaryView.class);
        when(b2.getId()).thenReturn(103L);
        when(b2.getTitle()).thenReturn("Heartbeat error");
        when(b2.getStatus()).thenReturn(BugStatus.RESOLVED);
        when(b2.getPriority()).thenReturn(BugPriority.LOW);
        when(b2.getOccurrenceCount()).thenReturn(2);
        when(b2.getSessionsAffected()).thenReturn(1L);
        when(b2.isRegression()).thenReturn(false);
        when(b2.getMinutesOpen()).thenReturn(45L);

        BugSummaryView b3 = mock(BugSummaryView.class);
        when(b3.getId()).thenReturn(106L);
        when(b3.getTitle()).thenReturn("Regression bug");
        when(b3.getStatus()).thenReturn(BugStatus.REOPENED);
        when(b3.getPriority()).thenReturn(BugPriority.CRITICAL);
        when(b3.getOccurrenceCount()).thenReturn(9);
        when(b3.getSessionsAffected()).thenReturn(3L);
        when(b3.isRegression()).thenReturn(true);

        when(viewRepo.findAll()).thenReturn(List.of(b1, b2, b3));
        when(eventRepo.findErrorEventTimestampsSince(any(LocalDateTime.class)))
                .thenReturn(List.of(LocalDateTime.now(), LocalDateTime.now().minusDays(1)));

        AnalyticsController controller = new AnalyticsController(viewRepo, eventRepo);
        var resp = controller.getOverview().getBody();

        assertNotNull(resp);
        assertEquals(3, resp.totalBugs());
        assertEquals(16, resp.totalOccurrences());
        assertEquals(1, resp.regressionCount());
        assertEquals(45.0, resp.meanMinutesToResolve());
        assertEquals(1L, resp.countsByStatus().get("OPEN"));
        assertEquals(1L, resp.countsByStatus().get("RESOLVED"));
        assertEquals(1L, resp.countsByStatus().get("REOPENED"));
        assertEquals(106L, resp.topBugsByOccurrences().get(0).id());
        assertEquals(14, resp.errorsPerDay().size());
    }
}

