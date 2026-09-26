package com.devcommand.devcommand.learning.dto;

import com.devcommand.devcommand.learning.entity.LearningStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * PUT /api/learning/{id} body - full replace of the editable fields, same
 * shape/required-ness as create (same PUT-as-full-replace assumption used
 * throughout the DSA/Tasks/Jobs modules). No owner field - there is
 * nowhere in this DTO the client could reassign a topic, since the
 * service loads it by (id, callerUserId) and only mutates the
 * already-owned managed entity.
 */
public record UpdateLearningTopicRequest(
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
