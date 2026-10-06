package com.devcommand.devcommand.dsa.dto;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class LeetCodeStatsResponse {
    private Integer totalSolved;
    private Integer easySolved;
    private Integer mediumSolved;
    private Integer hardSolved;
    private Integer ranking;
    private Instant lastUpdatedAt;
}
