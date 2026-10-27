package com.devcommand.devcommand.integrations.ai.orchestrator;

import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;

import java.util.List;
import java.util.Map;

public record OrchestratorResponse(
        OrchestratorStatus status,
        String responseMessage,
        String conversationId,
        List<ToolExecutionResult> results,
        String confirmationToken,
        Map<String, Object> pendingContext
) {
    public static OrchestratorResponse executed(String message, String conversationId, List<ToolExecutionResult> results) {
        return new OrchestratorResponse(OrchestratorStatus.EXECUTED, message, conversationId, results != null ? results : List.of(), null, null);
    }

    public static OrchestratorResponse executed(String message, List<ToolExecutionResult> results) {
        return executed(message, null, results);
    }

    public static OrchestratorResponse executed(String message, ToolExecutionResult singleResult) {
        return new OrchestratorResponse(OrchestratorStatus.EXECUTED, message, null, singleResult != null ? List.of(singleResult) : List.of(), null, null);
    }

    public static OrchestratorResponse awaitingConfirmation(String message, String token, Map<String, Object> pendingContext) {
        return new OrchestratorResponse(OrchestratorStatus.AWAITING_CONFIRMATION, message, null, List.of(), token, pendingContext);
    }

    public static OrchestratorResponse clarification(String message, Map<String, Object> pendingContext) {
        return new OrchestratorResponse(OrchestratorStatus.CLARIFICATION_REQUIRED, message, null, List.of(), null, pendingContext);
    }

    public static OrchestratorResponse clarification(String message, String conversationId, Map<String, Object> pendingContext) {
        return new OrchestratorResponse(OrchestratorStatus.CLARIFICATION_REQUIRED, message, conversationId, List.of(), null, pendingContext);
    }

    public static OrchestratorResponse cancelled(String message) {
        return new OrchestratorResponse(OrchestratorStatus.CANCELLED, message, null, List.of(), null, null);
    }

    public static OrchestratorResponse unsupported(String message) {
        return new OrchestratorResponse(OrchestratorStatus.UNSUPPORTED, message, null, List.of(), null, null);
    }

    public static OrchestratorResponse error(String message) {
        return new OrchestratorResponse(OrchestratorStatus.ERROR, message, null, List.of(), null, null);
    }
}
