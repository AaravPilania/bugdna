package com.bugdna.analytics;

import com.bugdna.bug.BugPriority;
import com.bugdna.bug.BugStatus;
import com.bugdna.bug.ErrorType;
import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.LocalDateTime;

@Entity
@Immutable
@Table(name = "bug_summary")
public class BugSummaryView {

    @Id
    private Long id;

    @Column(name = "project_id")
    private Long projectId;

    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type")
    private ErrorType errorType;

    @Enumerated(EnumType.STRING)
    private BugStatus status;

    @Enumerated(EnumType.STRING)
    private BugPriority priority;

    private String assignee;

    @Column(name = "occurrence_count")
    private int occurrenceCount;

    @Column(name = "is_regression")
    private boolean isRegression;

    @Column(name = "regression_count")
    private int regressionCount;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "sessions_affected")
    private long sessionsAffected;

    @Column(name = "comment_count")
    private long commentCount;

    @Column(name = "minutes_open")
    private Long minutesOpen;

    public BugSummaryView() {
    }

    public Long getId() {
        return id;
    }

    public Long getProjectId() {
        return projectId;
    }

    public String getTitle() {
        return title;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public BugStatus getStatus() {
        return status;
    }

    public BugPriority getPriority() {
        return priority;
    }

    public String getAssignee() {
        return assignee;
    }

    public int getOccurrenceCount() {
        return occurrenceCount;
    }

    public boolean isRegression() {
        return isRegression;
    }

    public int getRegressionCount() {
        return regressionCount;
    }

    public LocalDateTime getFirstSeenAt() {
        return firstSeenAt;
    }

    public LocalDateTime getLastSeenAt() {
        return lastSeenAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public long getSessionsAffected() {
        return sessionsAffected;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public Long getMinutesOpen() {
        return minutesOpen;
    }
}

