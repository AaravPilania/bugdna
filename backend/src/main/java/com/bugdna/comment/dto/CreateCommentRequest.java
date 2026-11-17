package com.bugdna.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
    @NotBlank(message = "Author is required")
    @Size(max = 80, message = "Author cannot exceed 80 characters")
    String author,

    @NotBlank(message = "Comment body is required")
    @Size(min = 1, max = 2000, message = "Comment body must be between 1 and 2000 characters")
    String body
) {}

