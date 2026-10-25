package com.devcommand.devcommand.integrations.ai.confirmation;

import java.time.Instant;
import java.util.Map;

public record PendingAction(
        String confirmationId,
        Long userId,
        String toolName,
        Map<String, Object> parameters,
        String summary,
        Instant createdAt,
        Instant expiresAt
) {
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
