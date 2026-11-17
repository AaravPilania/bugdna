package com.bugdna.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
    @NotBlank(message = "Comment body is required")
    @Size(min = 1, max = 2000, message = "Comment body must be between 1 and 2000 characters")
    String body
) {}

