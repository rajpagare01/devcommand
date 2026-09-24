package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * PUT /api/jobs/{id} body - full replace of the editable fields, same
 * shape/required-ness as create (same PUT-as-full-replace assumption used
 * throughout the DSA and Daily Tasks modules). No owner field - there is
 * nowhere in this DTO the client could reassign a job application, since
 * the service loads it by (id, callerUserId) and only mutates the
 * already-owned managed entity.
 */
public record UpdateJobApplicationRequest(
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
