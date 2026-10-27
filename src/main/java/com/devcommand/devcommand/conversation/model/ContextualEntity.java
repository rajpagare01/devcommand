package com.devcommand.devcommand.conversation.model;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;

/**
 * Structured record tracking a recently resolved/executed entity in conversation context.
 */
public record ContextualEntity(
        ContextType entityType,
        Long entityId,
        String title,
        String platform,
        Instant timestamp,
        String conversationId,
        Map<String, Object> metadata
) {
    public ContextualEntity {
        if (metadata == null) {
            metadata = Collections.emptyMap();
        }
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static ContextualEntity dsa(Long entityId, String title, String platform, String conversationId, Map<String, Object> metadata) {
        return new ContextualEntity(ContextType.LAST_DSA_PROBLEM, entityId, title, platform, Instant.now(), conversationId, metadata);
    }

    public static ContextualEntity task(Long entityId, String title, String conversationId, Map<String, Object> metadata) {
        return new ContextualEntity(ContextType.LAST_TASK, entityId, title, null, Instant.now(), conversationId, metadata);
    }

    public static ContextualEntity project(Long entityId, String title, String conversationId, Map<String, Object> metadata) {
        return new ContextualEntity(ContextType.LAST_PROJECT, entityId, title, null, Instant.now(), conversationId, metadata);
    }

    public static ContextualEntity job(Long entityId, String title, String conversationId, Map<String, Object> metadata) {
        return new ContextualEntity(ContextType.LAST_JOB_APPLICATION, entityId, title, null, Instant.now(), conversationId, metadata);
    }

    public static ContextualEntity learning(Long entityId, String title, String conversationId, Map<String, Object> metadata) {
        return new ContextualEntity(ContextType.LAST_LEARNING_TOPIC, entityId, title, null, Instant.now(), conversationId, metadata);
    }
}
