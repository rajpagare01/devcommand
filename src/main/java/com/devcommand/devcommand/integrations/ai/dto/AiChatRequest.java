package com.devcommand.devcommand.integrations.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiChatRequest(
        @NotBlank(message = "Message cannot be blank")
        String message,
        String conversationId,
        String conversationContext
) {
    public AiChatRequest(String message, String conversationContext) {
        this(message, null, conversationContext);
    }
}
