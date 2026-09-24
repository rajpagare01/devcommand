package com.devcommand.devcommand.dsa.dto;

import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record DsaProblemResponse(
        Long id,
        String title,
        String platform,
        String problemUrl,
        String topic,
        Difficulty difficulty,
        ProblemStatus status,
        LocalDate dateSolved,
        Integer timeTaken,
        String notes,
        LocalDate revisionDate,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
