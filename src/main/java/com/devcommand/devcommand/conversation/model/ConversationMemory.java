package com.devcommand.devcommand.conversation.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Encapsulates the conversation state: structured contextual entities and bounded message history.
 */
public record ConversationMemory(
        Map<ContextType, ContextualEntity> entities,
        List<ConversationMessage> recentMessages
) {
    public ConversationMemory {
        if (entities == null) {
            entities = Collections.emptyMap();
        }
        if (recentMessages == null) {
            recentMessages = Collections.emptyList();
        }
    }

    public static ConversationMemory empty() {
        return new ConversationMemory(Collections.emptyMap(), Collections.emptyList());
    }

    public Optional<ContextualEntity> getEntity(ContextType type) {
        return Optional.ofNullable(entities != null ? entities.get(type) : null);
    }
}
