package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

/** PATCH /api/jobs/{id}/status body. */
public record JobStatusUpdateRequest(
        @NotNull(message = "Status is required")
        ApplicationStatus status
) {
}
