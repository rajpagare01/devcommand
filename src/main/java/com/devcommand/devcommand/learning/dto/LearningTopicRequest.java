package com.devcommand.devcommand.learning.dto;

import com.devcommand.devcommand.learning.entity.LearningStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LearningTopicRequest(
        @NotBlank String technology,
        @NotBlank String topic,
        @NotNull @Min(0) @Max(100) Integer progress,
        @NotNull LearningStatus status,
        Double hoursSpent,
        String resourceUrl,
        String notes
) {
}
