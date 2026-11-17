package com.bugdna.event;

import com.bugdna.bug.BugOccurrence;
import com.bugdna.bug.BugOccurrenceRepository;
import com.bugdna.bug.BugRepository;
import com.bugdna.bug.dto.BugOccurrenceDto;
import com.bugdna.common.ResourceNotFoundException;
import com.bugdna.event.dto.TimelineEventDto;
import com.bugdna.event.dto.TimelineResponseDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimelineService {

    private static final int MAX_TIMELINE_EVENTS = 30;

    private final BugRepository bugRepository;
    private final BugOccurrenceRepository occurrenceRepository;
    private final EventRepository eventRepository;

    public TimelineService(
            BugRepository bugRepository,
            BugOccurrenceRepository occurrenceRepository,
            EventRepository eventRepository
    ) {
        this.bugRepository = bugRepository;
        this.occurrenceRepository = occurrenceRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<BugOccurrenceDto> getOccurrences(Long bugId) {
        if (!bugRepository.existsById(bugId)) {
            throw new ResourceNotFoundException("Bug not found with id: " + bugId);
        }
        return occurrenceRepository.findByBugIdOrderByOccurredAtDesc(bugId)
                .stream()
                .map(BugOccurrenceDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public TimelineResponseDto getTimeline(Long bugId, Long occurrenceId) {
        if (!bugRepository.existsById(bugId)) {
            throw new ResourceNotFoundException("Bug not found with id: " + bugId);
        }

        List<BugOccurrence> occurrences = occurrenceRepository.findByBugIdOrderByOccurredAtDesc(bugId);
        if (occurrences.isEmpty()) {
            return new TimelineResponseDto(bugId, null, null, null, false, List.of());
        }

        BugOccurrence target;
        if (occurrenceId != null) {
            target = occurrences.stream()
                    .filter(o -> o.getId().equals(occurrenceId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Occurrence not found for bug #" + bugId + ": " + occurrenceId));
        } else {
            // Default to the latest occurrence
            target = occurrences.get(0);
        }

        Event triggerEvent = target.getTriggerEvent();
        LocalDateTime triggerTime = triggerEvent.getOccurredAt() != null
                ? triggerEvent.getOccurredAt()
                : target.getOccurredAt();

        // Click-to-crash window: [trigger - 120s, trigger + 10s], same session only, max 30 events ascending
        LocalDateTime windowStart = triggerTime.minusSeconds(120);
        LocalDateTime windowEnd = triggerTime.plusSeconds(10);

        List<Event> sessionEvents = eventRepository.findTimelineEvents(
                target.getSession().getId(),
                windowStart,
                windowEnd
        );

        // If > 30 events exist in window, keep the 30 events closest to/leading up to the crash
        List<Event> capped = sessionEvents.size() > MAX_TIMELINE_EVENTS
                ? sessionEvents.subList(sessionEvents.size() - MAX_TIMELINE_EVENTS, sessionEvents.size())
                : sessionEvents;

        List<TimelineEventDto> timelineEvents = capped.stream()
                .map(ev -> TimelineEventDto.fromEntity(ev, triggerTime, triggerEvent.getId()))
                .toList();

        return new TimelineResponseDto(
                bugId,
                target.getId(),
                target.getSession().getSessionId(),
                triggerTime,
                target.isWasRegression(),
                timelineEvents
        );
    }
}

