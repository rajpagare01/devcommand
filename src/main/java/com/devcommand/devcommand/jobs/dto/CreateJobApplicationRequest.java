package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * POST /api/jobs body. No userId field on purpose - the owner is always
 * taken from the authenticated security context in the service layer.
 */
public record CreateJobApplicationRequest(
        @NotBlank(message = "Company is required")
        String company,

        @NotBlank(message = "Role is required")
        String role,

        String location,
        String jobUrl,
        String source,
        String salary,

        @NotNull(message = "Application date is required")
        LocalDate applicationDate,

        @NotNull(message = "Status is required")
        ApplicationStatus status,

        String notes
) {
}
