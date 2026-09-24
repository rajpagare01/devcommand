package com.devcommand.devcommand.jobs.dto;

import com.devcommand.devcommand.jobs.entity.InterviewStatus;

import java.time.LocalDateTime;

/** No createdAt/updatedAt - InterviewRound doesn't have those fields (see entity Javadoc). */
public record InterviewRoundResponse(
        Long id,
        Integer roundNumber,
        String roundType,
        LocalDateTime scheduledAt,
        InterviewStatus status,
        String feedback,
        String notes
) {
}
