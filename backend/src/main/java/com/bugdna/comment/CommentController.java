package com.bugdna.comment;

import com.bugdna.bug.Bug;
import com.bugdna.bug.BugRepository;
import com.bugdna.comment.dto.CommentDto;
import com.bugdna.comment.dto.CreateCommentRequest;
import com.bugdna.comment.dto.UpdateCommentRequest;
import com.bugdna.common.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentRepository commentRepository;
    private final BugRepository bugRepository;

    public CommentController(CommentRepository commentRepository, BugRepository bugRepository) {
        this.commentRepository = commentRepository;
        this.bugRepository = bugRepository;
    }

    @GetMapping("/bugs/{bugId}/comments")
    public ResponseEntity<List<CommentDto>> getCommentsForBug(@PathVariable Long bugId) {
        if (!bugRepository.existsById(bugId)) {
            throw new ResourceNotFoundException("Bug not found with id: " + bugId);
        }
        List<CommentDto> list = commentRepository.findByBugIdOrderByCreatedAtAsc(bugId)
                .stream()
                .map(CommentDto::fromEntity)
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/bugs/{bugId}/comments")
    @Transactional
    public ResponseEntity<CommentDto> createComment(
            @PathVariable Long bugId,
            @Valid @RequestBody CreateCommentRequest request
    ) {
        Bug bug = bugRepository.findById(bugId)
                .orElseThrow(() -> new ResourceNotFoundException("Bug not found with id: " + bugId));

        Comment comment = new Comment(bug, request.author().trim(), request.body().trim());
        Comment saved = commentRepository.saveAndFlush(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(CommentDto.fromEntity(saved));
    }

    @PatchMapping("/comments/{id}")
    @Transactional
    public ResponseEntity<CommentDto> updateComment(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCommentRequest request
    ) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found with id: " + id));

        comment.setBody(request.body().trim());
        Comment saved = commentRepository.saveAndFlush(comment);
        return ResponseEntity.ok(CommentDto.fromEntity(saved));
    }

    @DeleteMapping("/comments/{id}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteComment(@PathVariable Long id) {
        if (!commentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Comment not found with id: " + id);
        }
        commentRepository.deleteById(id);
        commentRepository.flush();
        return ResponseEntity.ok(Map.of("message", "Comment deleted successfully", "id", id));
    }
}

