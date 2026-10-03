package com.devcommand.devcommand.learning.dto;

import com.devcommand.devcommand.learning.entity.LearningStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * PUT /api/learning/{id} body - full replace.
 */
public record UpdateLearningTopicRequest(
        @NotBlank(message = "Technology is required")
        @Size(max = 255, message = "Technology must be at most 255 characters")
        String technology,

        @NotBlank(message = "Topic is required")
        @Size(max = 255, message = "Topic must be at most 255 characters")
        String topic,

        @NotNull(message = "Progress is required")
        @Min(value = 0, message = "Progress cannot be less than 0")
        @Max(value = 100, message = "Progress cannot be greater than 100")
        Integer progress,

        @NotNull(message = "Status is required")
        LearningStatus status,

        @PositiveOrZero(message = "Hours spent cannot be negative")
        Double hoursSpent,

        @Size(max = 2048, message = "Resource URL must be at most 2048 characters")
        String resourceUrl,

        @Size(max = 2000, message = "Notes must be at most 2000 characters")
        String notes
) {
}
