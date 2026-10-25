package com.devcommand.devcommand.integrations.ai.dto;

import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorResponse;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorStatus;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;

import java.util.List;
import java.util.Map;

public record AiChatResponse(
        OrchestratorStatus status,
        String message,
        List<ToolExecutionResult> results,
        String confirmationToken,
        Map<String, Object> pendingContext
) {
    public static AiChatResponse from(OrchestratorResponse resp) {
        return new AiChatResponse(
                resp.status(),
                resp.responseMessage(),
                resp.results(),
                resp.confirmationToken(),
                resp.pendingContext()
        );
    }
}
