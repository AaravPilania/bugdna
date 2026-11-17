package com.bugdna.analytics;

import com.bugdna.bug.BugPriority;
import com.bugdna.bug.BugStatus;
import com.bugdna.event.EventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    public record TopBugDto(
            Long id,
            String title,
            BugStatus status,
            BugPriority priority,
            int occurrenceCount,
            long sessionsAffected,
            boolean isRegression
    ) {}

    public record DailyErrorCountDto(
            String date,
            long count
    ) {}

    public record AnalyticsOverviewResponse(
            long totalBugs,
            long totalOccurrences,
            long regressionCount,
            Double meanMinutesToResolve,
            Map<String, Long> countsByStatus,
            Map<String, Long> countsByPriority,
            List<TopBugDto> topBugsByOccurrences,
            List<DailyErrorCountDto> errorsPerDay
    ) {}

    private final BugSummaryViewRepository bugSummaryViewRepository;
    private final EventRepository eventRepository;

    public AnalyticsController(
            BugSummaryViewRepository bugSummaryViewRepository,
            EventRepository eventRepository
    ) {
        this.bugSummaryViewRepository = bugSummaryViewRepository;
        this.eventRepository = eventRepository;
    }

    @GetMapping("/overview")
    @Transactional(readOnly = true)
    public ResponseEntity<AnalyticsOverviewResponse> getOverview() {
        List<BugSummaryView> summaries = bugSummaryViewRepository.findAll();

        long totalBugs = summaries.size();
        long totalOccurrences = summaries.stream().mapToLong(BugSummaryView::getOccurrenceCount).sum();
        long regressionCount = summaries.stream().filter(BugSummaryView::isRegression).count();

        Map<String, Long> countsByStatus = new LinkedHashMap<>();
        for (BugStatus s : BugStatus.values()) {
            countsByStatus.put(s.name(), 0L);
        }
        Map<String, Long> countsByPriority = new LinkedHashMap<>();
        for (BugPriority p : List.of(BugPriority.CRITICAL, BugPriority.HIGH, BugPriority.MEDIUM, BugPriority.LOW)) {
            countsByPriority.put(p.name(), 0L);
        }

        List<Long> resolvedMinutes = new ArrayList<>();
        for (BugSummaryView v : summaries) {
            if (v.getStatus() != null) {
                countsByStatus.merge(v.getStatus().name(), 1L, Long::sum);
            }
            if (v.getPriority() != null) {
                countsByPriority.merge(v.getPriority().name(), 1L, Long::sum);
            }
            if ((v.getStatus() == BugStatus.RESOLVED || v.getStatus() == BugStatus.CLOSED)
                    && v.getMinutesOpen() != null) {
                resolvedMinutes.add(v.getMinutesOpen());
            }
        }

        Double meanMinutesToResolve = resolvedMinutes.isEmpty()
                ? null
                : Math.round(resolvedMinutes.stream().mapToLong(Long::longValue).average().orElse(0.0) * 10.0) / 10.0;

        List<TopBugDto> topBugs = summaries.stream()
                .sorted(Comparator.comparingInt(BugSummaryView::getOccurrenceCount).reversed()
                        .thenComparing(BugSummaryView::getId))
                .limit(5)
                .map(v -> new TopBugDto(
                        v.getId(),
                        v.getTitle(),
                        v.getStatus(),
                        v.getPriority(),
                        v.getOccurrenceCount(),
                        v.getSessionsAffected(),
                        v.isRegression()
                ))
                .toList();

        // Last 14 calendar days (including today), zero-filled
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(13);
        Map<LocalDate, Long> dailyCounts = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) {
            dailyCounts.put(startDate.plusDays(i), 0L);
        }

        List<LocalDateTime> errorTimestamps = eventRepository.findErrorEventTimestampsSince(startDate.atStartOfDay());
        for (LocalDateTime ts : errorTimestamps) {
            if (ts != null) {
                LocalDate day = ts.toLocalDate();
                if (dailyCounts.containsKey(day)) {
                    dailyCounts.merge(day, 1L, Long::sum);
                }
            }
        }

        List<DailyErrorCountDto> errorsPerDay = dailyCounts.entrySet().stream()
                .map(e -> new DailyErrorCountDto(e.getKey().toString(), e.getValue()))
                .toList();

        return ResponseEntity.ok(new AnalyticsOverviewResponse(
                totalBugs,
                totalOccurrences,
                regressionCount,
                meanMinutesToResolve,
                countsByStatus,
                countsByPriority,
                topBugs,
                errorsPerDay
        ));
    }
}

