package com.devcommand.devcommand.dsa.dto;

import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeetCodeIntegrationResponse {
    private Long id;
    private String username;
    private String profileUrl;
    private ExternalDsaAccountStatus status;
    private Instant lastSyncedAt;

    // Direct stats fields expected by frontend
    private Integer totalSolved;
    private Integer easy;
    private Integer medium;
    private Integer hard;
    private Integer easySolved;
    private Integer mediumSolved;
    private Integer hardSolved;
    private Integer ranking;

    // Nested stats object for backward compatibility
    private LeetCodeStatsResponse stats;
}
