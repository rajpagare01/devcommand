package com.devcommand.devcommand.dsa.dto;

import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountStatus;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class LeetCodeIntegrationResponse {
    private Long id;
    private String username;
    private String profileUrl;
    private ExternalDsaAccountStatus status;
    private Instant lastSyncedAt;
    private LeetCodeStatsResponse stats;
}
