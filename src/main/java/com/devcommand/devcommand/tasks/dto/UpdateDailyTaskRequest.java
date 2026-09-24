package com.devcommand.devcommand.tasks.dto;

import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * PUT /api/tasks/{id} body - full replace of the editable fields, same
 * shape/required-ness as create (consistent with how the DSA module's
 * UpdateDsaProblemRequest was designed). There is no owner field here
 * either: ownership can never be changed via this endpoint, since the
 * service loads the task by (id, callerUserId) and only ever mutates the
 * already-owned managed entity - there is nowhere in this DTO or the
 * mapper that a different user id could even be applied.
 */
public record UpdateDailyTaskRequest(
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
