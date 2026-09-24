package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record JobApplicationRequest(
        @NotBlank String company,
        @NotBlank String role,
        String location,
        String jobUrl,
        String source,
        String salary,
        LocalDate applicationDate,
        @NotNull ApplicationStatus status,
        String notes
) {
}
