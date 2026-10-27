package com.devcommand.devcommand.conversation.model;

import java.time.Instant;

/**
 * Represents an individual message turn in bounded conversation history.
 */
public record ConversationMessage(
        String role,
        String content,
        Instant timestamp
) {
    public ConversationMessage {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static ConversationMessage user(String content) {
        return new ConversationMessage("user", content, Instant.now());
    }

    public static ConversationMessage assistant(String content) {
        return new ConversationMessage("assistant", content, Instant.now());
    }
}
