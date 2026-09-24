package com.devcommand.devcommand.dsa.dto;

import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

/**
 * PUT /api/dsa/{id} body.
 *
 * Assumption: PUT is treated as a full replace of the editable fields (the
 * same shape/required-ness as create), rather than a partial PATCH-style
 * update - the prompt said "allow appropriate fields to be updated" without
 * specifying partial semantics, and PUT's own HTTP semantics are full
 * replacement. PATCH is reserved for the two dedicated status-transition
 * endpoints (/solve, /revision) that already exist in the spec.
 */
public record UpdateDsaProblemRequest(
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
