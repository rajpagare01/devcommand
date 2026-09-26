package com.devcommand.devcommand.projects.dto;

import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProjectTaskRequest(
        @NotBlank String title,
        String description,
        @NotNull ProjectTaskStatus status,
        @NotNull ProjectTaskPriority priority,
        LocalDate dueDate
) {
}
