package com.devcommand.devcommand.tasks.dto;

import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * POST /api/tasks body. No userId/owner field on purpose - the owner is
 * always taken from the authenticated security context in the service
 * layer, never from client input.
 */
public record CreateDailyTaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        String description,

        @NotNull(message = "Category is required")
        TaskCategory category,

        @NotNull(message = "Priority is required")
        TaskPriority priority,

        @NotNull(message = "Status is required")
        DailyTaskStatus status,

        LocalDate dueDate
) {
}
