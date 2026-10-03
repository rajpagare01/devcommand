package com.devcommand.devcommand.projects.dto;

import com.devcommand.devcommand.projects.entity.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ProjectRequest(
        @NotBlank
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        @Size(max = 2048, message = "GitHub URL must be at most 2048 characters")
        String githubUrl,

        @Size(max = 2048, message = "Live URL must be at most 2048 characters")
        String liveUrl,

        @NotNull ProjectStatus status,
        LocalDate startDate,
        LocalDate endDate
) {
}
