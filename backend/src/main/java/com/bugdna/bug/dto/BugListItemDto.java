package com.bugdna.bug.dto;

import com.bugdna.bug.Bug;
import com.bugdna.bug.BugPriority;
import com.bugdna.bug.BugStatus;
import com.bugdna.bug.ErrorType;

import java.time.LocalDateTime;

public record BugListItemDto(
    Long id,
    String title,
    ErrorType errorType,
    BugStatus status,
    BugPriority priority,
    String assignee,
    int occurrenceCount,
    boolean isRegression,
    int regressionCount,
    LocalDateTime lastSeenAt,
    String fingerprintShort,
    String fingerprint
) {
    public static BugListItemDto fromEntity(Bug bug) {
        String fp = bug.getFingerprint();
        String shortFp = (fp != null && fp.length() > 12) ? fp.substring(0, 12) : fp;
        return new BugListItemDto(
            bug.getId(),
            bug.getTitle(),
            bug.getErrorType(),
            bug.getStatus(),
            bug.getPriority(),
            bug.getAssignee(),
            bug.getOccurrenceCount(),
            bug.isRegression(),
            bug.getRegressionCount(),
            bug.getLastSeenAt(),
            shortFp,
            fp
        );
    }
}

