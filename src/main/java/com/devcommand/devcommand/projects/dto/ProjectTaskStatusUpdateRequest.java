package com.devcommand.devcommand.projects.dto;

import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import jakarta.validation.constraints.NotNull;

public record ProjectTaskStatusUpdateRequest(
        @NotNull ProjectTaskStatus status
) {
}
