package com.devcommand.devcommand.learning.dto;

import com.devcommand.devcommand.learning.entity.LearningStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * POST /api/learning body. No userId field on purpose - the owner is
 * always taken from the authenticated security context in the service
 * layer, never from client input.
 */
public record CreateLearningTopicRequest(
        @NotBlank(message = "Technology is required")
        String technology,

        @NotBlank(message = "Topic is required")
        String topic,

        @NotNull(message = "Progress is required")
        @Min(value = 0, message = "Progress cannot be less than 0")
        @Max(value = 100, message = "Progress cannot be greater than 100")
        Integer progress,

        @NotNull(message = "Status is required")
        LearningStatus status,

        @PositiveOrZero(message = "Hours spent cannot be negative")
        Double hoursSpent,

        String resourceUrl,
        String notes
) {
}
