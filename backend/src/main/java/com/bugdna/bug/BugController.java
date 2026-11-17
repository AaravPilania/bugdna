package com.bugdna.bug;

import com.bugdna.bug.dto.BugDetailDto;
import com.bugdna.bug.dto.BugListItemDto;
import com.bugdna.bug.dto.BugOccurrenceDto;
import com.bugdna.bug.dto.BugPatchRequest;
import com.bugdna.event.TimelineService;
import com.bugdna.event.dto.TimelineResponseDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bugs")
public class BugController {

    private final BugService bugService;
    private final TimelineService timelineService;

    public BugController(BugService bugService, TimelineService timelineService) {
        this.bugService = bugService;
        this.timelineService = timelineService;
    }

    @GetMapping
    public ResponseEntity<List<BugListItemDto>> listBugs(
            @RequestParam(required = false) BugStatus status,
            @RequestParam(required = false) BugPriority priority,
            @RequestParam(required = false) ErrorType errorType,
            @RequestParam(required = false) String q,
            @RequestParam(required = false, defaultValue = "lastSeen") String sort
    ) {
        List<BugListItemDto> list = bugService.listBugs(status, priority, errorType, q, sort);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BugDetailDto> getBug(@PathVariable Long id) {
        BugDetailDto bug = bugService.getBugDetail(id);
        return ResponseEntity.ok(bug);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<BugDetailDto> patchBug(
            @PathVariable Long id,
            @Valid @RequestBody BugPatchRequest request
    ) {
        BugDetailDto updated = bugService.patchBug(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteBug(@PathVariable Long id) {
        bugService.deleteBug(id);
        return ResponseEntity.ok(Map.of("message", "Bug deleted successfully", "id", id));
    }

    @GetMapping("/{id}/occurrences")
    public ResponseEntity<List<BugOccurrenceDto>> getOccurrences(@PathVariable Long id) {
        return ResponseEntity.ok(timelineService.getOccurrences(id));
    }

    @GetMapping("/{id}/timeline")
    public ResponseEntity<TimelineResponseDto> getTimeline(
            @PathVariable Long id,
            @RequestParam(required = false) Long occurrenceId
    ) {
        return ResponseEntity.ok(timelineService.getTimeline(id, occurrenceId));
    }
}
