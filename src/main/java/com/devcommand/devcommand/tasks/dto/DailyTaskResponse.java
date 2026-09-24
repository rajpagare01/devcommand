package com.devcommand.devcommand.tasks.dto;

import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DailyTaskResponse(
        Long id,
        String title,
        String description,
        TaskCategory category,
        TaskPriority priority,
        DailyTaskStatus status,
        LocalDate dueDate,
        LocalDateTime completedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
