package com.devcommand.devcommand.projects.dto;

import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectTaskResponse(
        Long id,
        String title,
        String description,
        ProjectTaskStatus status,
        ProjectTaskPriority priority,
        LocalDate dueDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
