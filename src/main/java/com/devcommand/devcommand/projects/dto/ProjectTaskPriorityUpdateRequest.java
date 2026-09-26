package com.devcommand.devcommand.projects.dto;

import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import jakarta.validation.constraints.NotNull;

public record ProjectTaskPriorityUpdateRequest(
        @NotNull ProjectTaskPriority priority
) {
}
