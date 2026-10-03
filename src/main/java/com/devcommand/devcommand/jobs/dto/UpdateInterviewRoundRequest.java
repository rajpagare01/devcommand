package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.InterviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/** PUT /api/jobs/{jobId}/interviews/{roundId} body - full replace. */
public record UpdateInterviewRoundRequest(
        @NotNull(message = "Round number is required")
        @Positive(message = "Round number must be positive")
        Integer roundNumber,

        @NotBlank(message = "Round type is required")
        @Size(max = 255, message = "Round type must be at most 255 characters")
        String roundType,

        LocalDateTime scheduledAt,

        @NotNull(message = "Status is required")
        InterviewStatus status,

        @Size(max = 2000, message = "Feedback must be at most 2000 characters")
        String feedback,

        @Size(max = 2000, message = "Notes must be at most 2000 characters")
        String notes
) {
}
