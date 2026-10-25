package com.devcommand.devcommand.integrations.ai.confirmation;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConfirmationManager {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private final Map<String, PendingAction> pendingById = new ConcurrentHashMap<>();
    private final Map<Long, String> activeByUserId = new ConcurrentHashMap<>();

    public PendingAction createPendingAction(Long userId, String toolName, Map<String, Object> parameters, String summary) {
        String token = UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(DEFAULT_TTL);
        PendingAction pending = new PendingAction(token, userId, toolName, parameters, summary, now, expiresAt);

        pendingById.put(token.toLowerCase(), pending);
        activeByUserId.put(userId, token.toLowerCase());
        return pending;
    }

    public Optional<PendingAction> getActivePendingAction(Long userId) {
        cleanExpired();
        String tokenId = activeByUserId.get(userId);
        if (tokenId == null) {
            return Optional.empty();
        }
        PendingAction pending = pendingById.get(tokenId);
        if (pending == null || pending.isExpired()) {
            activeByUserId.remove(userId);
            if (pending != null) pendingById.remove(tokenId);
            return Optional.empty();
        }
        return Optional.of(pending);
    }

    public Optional<PendingAction> getPendingAction(String confirmationId, Long userId) {
        if (confirmationId == null) return Optional.empty();
        cleanExpired();
        PendingAction pending = pendingById.get(confirmationId.trim().toLowerCase());
        if (pending != null && pending.userId().equals(userId) && !pending.isExpired()) {
            return Optional.of(pending);
        }
        return Optional.empty();
    }

    public Optional<PendingAction> consume(String confirmationId, Long userId) {
        if (confirmationId == null || userId == null) return Optional.empty();
        cleanExpired();
        String key = confirmationId.trim().toLowerCase();
        PendingAction existing = pendingById.get(key);
        if (existing != null && existing.userId().equals(userId) && !existing.isExpired()) {
            pendingById.remove(key);
            activeByUserId.remove(userId, key);
            return Optional.of(existing);
        }
        return Optional.empty();
    }

    public Optional<PendingAction> consumeActive(Long userId) {
        cleanExpired();
        String tokenId = activeByUserId.remove(userId);
        if (tokenId != null) {
            PendingAction pending = pendingById.remove(tokenId);
            if (pending != null && !pending.isExpired()) {
                return Optional.of(pending);
            }
        }
        return Optional.empty();
    }

    public boolean cancelActive(Long userId) {
        String tokenId = activeByUserId.remove(userId);
        if (tokenId != null) {
            pendingById.remove(tokenId);
            return true;
        }
        return false;
    }

    public void cleanExpired() {
        Instant now = Instant.now();
        pendingById.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }
}
