package com.devcommand.devcommand.learning.dto;

import com.devcommand.devcommand.learning.entity.LearningStatus;

import java.time.LocalDateTime;

public record LearningTopicResponse(
        Long id,
        String technology,
        String topic,
        Integer progress,
        LearningStatus status,
        Double hoursSpent,
        String resourceUrl,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
