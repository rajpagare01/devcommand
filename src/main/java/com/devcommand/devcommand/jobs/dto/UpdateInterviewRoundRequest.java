package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.InterviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/** PUT /api/jobs/{jobId}/interviews/{roundId} body - full replace, same shape as create. */
public record UpdateInterviewRoundRequest(
        @NotNull(message = "Round number is required")
        @Positive(message = "Round number must be positive")
        Integer roundNumber,

        @NotBlank(message = "Round type is required")
        String roundType,

        LocalDateTime scheduledAt,

        @NotNull(message = "Status is required")
        InterviewStatus status,

        String feedback,
        String notes
) {
}
