package com.bugdna.activity;

import com.bugdna.bug.BugRepository;
import com.bugdna.comment.CommentRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/sync")
public class SyncController {

    public record SyncStateResponse(
            Long maxActivityId,
            LocalDateTime bugsMaxUpdatedAt,
            long bugCount,
            long commentCount,
            LocalDateTime serverTime
    ) {}

    private final ActivityLogRepository activityLogRepository;
    private final BugRepository bugRepository;
    private final CommentRepository commentRepository;

    public SyncController(
            ActivityLogRepository activityLogRepository,
            BugRepository bugRepository,
            CommentRepository commentRepository
    ) {
        this.activityLogRepository = activityLogRepository;
        this.bugRepository = bugRepository;
        this.commentRepository = commentRepository;
    }

    @GetMapping("/state")
    public ResponseEntity<SyncStateResponse> getSyncState() {
        Long maxActId = activityLogRepository.findMaxId();
        LocalDateTime maxUpdated = bugRepository.findMaxUpdatedAt();
        long bugCount = bugRepository.count();
        long commentCount = commentRepository.count();

        SyncStateResponse response = new SyncStateResponse(
                maxActId != null ? maxActId : 0L,
                maxUpdated,
                bugCount,
                commentCount,
                LocalDateTime.now()
        );
        return ResponseEntity.ok(response);
    }
}

