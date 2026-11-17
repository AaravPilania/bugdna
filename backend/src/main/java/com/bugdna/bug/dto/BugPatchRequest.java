package com.bugdna.bug.dto;

import com.bugdna.bug.BugPriority;
import com.bugdna.bug.BugStatus;

public record BugPatchRequest(
    BugStatus status,
    BugPriority priority,
    String assignee,
    String title
) {}

