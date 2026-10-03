package com.devcommand.devcommand.dsa.dto;

import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * PUT /api/dsa/{id} body.
 */
public record UpdateDsaProblemRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must be at most 255 characters")
        String title,

        @NotBlank(message = "Platform is required")
        @Size(max = 255, message = "Platform must be at most 255 characters")
        String platform,

        @Size(max = 2048, message = "Problem URL must be at most 2048 characters")
        String problemUrl,

        @NotBlank(message = "Topic is required")
        @Size(max = 255, message = "Topic must be at most 255 characters")
        String topic,

        @NotNull(message = "Difficulty is required")
        Difficulty difficulty,

        @NotNull(message = "Status is required")
        ProblemStatus status,

        LocalDate dateSolved,

        @Positive(message = "Time taken must be a positive number of minutes")
        Integer timeTaken,

        @Size(max = 2000, message = "Notes must be at most 2000 characters")
        String notes,

        LocalDate revisionDate
) {
}
