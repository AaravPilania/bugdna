package com.bugdna.bug;

import com.bugdna.bug.dto.BugDetailDto;
import com.bugdna.bug.dto.BugListItemDto;
import com.bugdna.bug.dto.BugPatchRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/bugs")
public class BugController {

    private final BugService bugService;
    private final BugOccurrenceRepository bugOccurrenceRepository;

    public BugController(BugService bugService, BugOccurrenceRepository bugOccurrenceRepository) {
        this.bugService = bugService;
        this.bugOccurrenceRepository = bugOccurrenceRepository;
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
    public ResponseEntity<List<BugOccurrence>> getOccurrences(@PathVariable Long id) {
        List<BugOccurrence> occurrences = bugOccurrenceRepository.findByBugIdOrderByOccurredAtDesc(id);
        return ResponseEntity.ok(occurrences);
    }
}

