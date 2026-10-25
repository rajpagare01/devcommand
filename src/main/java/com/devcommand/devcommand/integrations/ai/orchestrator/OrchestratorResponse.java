package com.devcommand.devcommand.integrations.ai.orchestrator;

import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;

import java.util.List;
import java.util.Map;

public record OrchestratorResponse(
        OrchestratorStatus status,
        String responseMessage,
        List<ToolExecutionResult> results,
        String confirmationToken,
        Map<String, Object> pendingContext
) {
    public static OrchestratorResponse executed(String message, List<ToolExecutionResult> results) {
        return new OrchestratorResponse(OrchestratorStatus.EXECUTED, message, results != null ? results : List.of(), null, null);
    }

    public static OrchestratorResponse executed(String message, ToolExecutionResult singleResult) {
        return new OrchestratorResponse(OrchestratorStatus.EXECUTED, message, singleResult != null ? List.of(singleResult) : List.of(), null, null);
    }

    public static OrchestratorResponse awaitingConfirmation(String message, String token, Map<String, Object> pendingContext) {
        return new OrchestratorResponse(OrchestratorStatus.AWAITING_CONFIRMATION, message, List.of(), token, pendingContext);
    }

    public static OrchestratorResponse clarification(String message, Map<String, Object> pendingContext) {
        return new OrchestratorResponse(OrchestratorStatus.CLARIFICATION_REQUIRED, message, List.of(), null, pendingContext);
    }

    public static OrchestratorResponse cancelled(String message) {
        return new OrchestratorResponse(OrchestratorStatus.CANCELLED, message, List.of(), null, null);
    }

    public static OrchestratorResponse unsupported(String message) {
        return new OrchestratorResponse(OrchestratorStatus.UNSUPPORTED, message, List.of(), null, null);
    }

    public static OrchestratorResponse error(String message) {
        return new OrchestratorResponse(OrchestratorStatus.ERROR, message, List.of(), null, null);
    }
}
