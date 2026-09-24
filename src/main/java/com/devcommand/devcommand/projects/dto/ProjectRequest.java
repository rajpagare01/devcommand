package com.devcommand.devcommand.projects.dto;

import com.devcommand.devcommand.projects.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank String name,
        String description,
        String githubUrl,
        String liveUrl,
        @NotNull ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate
) {
}
