package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * PUT /api/jobs/{id} body - full replace.
 */
public record UpdateJobApplicationRequest(
        @NotBlank(message = "Company is required")
        @Size(max = 255, message = "Company must be at most 255 characters")
        String company,

        @NotBlank(message = "Role is required")
        @Size(max = 255, message = "Role must be at most 255 characters")
        String role,

        @Size(max = 255, message = "Location must be at most 255 characters")
        String location,

        @Size(max = 2048, message = "Job URL must be at most 2048 characters")
        String jobUrl,

        @Size(max = 255, message = "Source must be at most 255 characters")
        String source,

        @Size(max = 255, message = "Salary must be at most 255 characters")
        String salary,

        @NotNull(message = "Application date is required")
        LocalDate applicationDate,

        @NotNull(message = "Status is required")
        ApplicationStatus status,

        @Size(max = 2000, message = "Notes must be at most 2000 characters")
        String notes
) {
}
