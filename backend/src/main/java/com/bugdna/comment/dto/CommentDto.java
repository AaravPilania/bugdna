package com.bugdna.comment.dto;

import com.bugdna.comment.Comment;
import java.time.LocalDateTime;

public record CommentDto(
    Long id,
    Long bugId,
    String author,
    String body,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static CommentDto fromEntity(Comment c) {
        return new CommentDto(
            c.getId(),
            c.getBug() != null ? c.getBug().getId() : null,
            c.getAuthor(),
            c.getBody(),
            c.getCreatedAt(),
            c.getUpdatedAt()
        );
    }
}

