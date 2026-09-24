package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.InterviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDateTime;

/**
 * POST /api/jobs/{jobId}/interviews body. No jobApplicationId/userId
 * fields - the parent job comes from the URL path, and its ownership is
 * verified in the service layer before this request is ever applied. The
 * client cannot attach an interview round to a job it doesn't own by
 * supplying a different ID here, because there's no ID field to supply.
 */
public record CreateInterviewRoundRequest(
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
