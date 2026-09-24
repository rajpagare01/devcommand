package com.devcommand.devcommand.dsa.dto;

import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * POST /api/dsa body. There is no userId field here on purpose - the owner
 * is always taken from the authenticated security context in the service
 * layer, never from client input.
 */
public record CreateDsaProblemRequest(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "Platform is required")
        String platform,

        String problemUrl,

        @NotBlank(message = "Topic is required")
        String topic,

        @NotNull(message = "Difficulty is required")
        Difficulty difficulty,

        @NotNull(message = "Status is required")
        ProblemStatus status,

        LocalDate dateSolved,

        @Positive(message = "Time taken must be a positive number of minutes")
        Integer timeTaken,

        String notes,

        LocalDate revisionDate
) {
}
