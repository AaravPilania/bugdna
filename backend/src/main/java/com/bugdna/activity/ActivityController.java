package com.bugdna.activity;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/activity")
public class ActivityController {

    private final ActivityLogRepository activityLogRepository;

    public ActivityController(ActivityLogRepository activityLogRepository) {
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public ResponseEntity<List<ActivityLog>> getActivity(
            @RequestParam(required = false) Long afterId,
            @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        PageRequest pageRequest = PageRequest.of(0, safeLimit);

        List<ActivityLog> items;
        if (afterId != null && afterId > 0) {
            items = activityLogRepository.findNewerThan(afterId, pageRequest);
        } else {
            items = activityLogRepository.findRecent(pageRequest);
        }

        return ResponseEntity.ok(items);
    }
}

