package com.devcommand.devcommand.integrations.ai.orchestrator;

public record OrchestratorRequest(
        Long userId,
        String message,
        String conversationId,
        String conversationContext
) {
    public OrchestratorRequest(Long userId, String message, String conversationContext) {
        this(userId, message, null, conversationContext);
    }

    public OrchestratorRequest(Long userId, String message) {
        this(userId, message, null, null);
    }
}
