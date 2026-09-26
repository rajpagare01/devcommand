package com.devcommand.devcommand.projects.dto;

import com.devcommand.devcommand.projects.entity.ProjectStatus;
import jakarta.validation.constraints.NotNull;

public record ProjectStatusUpdateRequest(
        @NotNull ProjectStatus status
) {
}
