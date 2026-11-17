package com.bugdna.bug.dto;

import com.bugdna.bug.Bug;
import com.bugdna.bug.BugPriority;
import com.bugdna.bug.BugStatus;
import com.bugdna.bug.ErrorType;

import java.time.LocalDateTime;

public record BugDetailDto(
    Long id,
    Long projectId,
    String fingerprint,
    String title,
    ErrorType errorType,
    String errorCode,
    String httpMethod,
    String endpoint,
    String pagePath,
    BugStatus status,
    BugPriority priority,
    String assignee,
    int occurrenceCount,
    boolean isRegression,
    int regressionCount,
    LocalDateTime firstSeenAt,
    LocalDateTime lastSeenAt,
    LocalDateTime resolvedAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    long sessionsAffected,
    long commentCount,
    Long minutesOpen
) {
    public static BugDetailDto fromEntity(Bug bug, long sessionsAffected, long commentCount, Long minutesOpen) {
        return new BugDetailDto(
            bug.getId(),
            bug.getProject() != null ? bug.getProject().getId() : null,
            bug.getFingerprint(),
            bug.getTitle(),
            bug.getErrorType(),
            bug.getErrorCode(),
            bug.getHttpMethod(),
            bug.getEndpoint(),
            bug.getPagePath(),
            bug.getStatus(),
            bug.getPriority(),
            bug.getAssignee(),
            bug.getOccurrenceCount(),
            bug.isRegression(),
            bug.getRegressionCount(),
            bug.getFirstSeenAt(),
            bug.getLastSeenAt(),
            bug.getResolvedAt(),
            bug.getCreatedAt(),
            bug.getUpdatedAt(),
            sessionsAffected,
            commentCount,
            minutesOpen
        );
    }
}

