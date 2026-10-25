package com.devcommand.devcommand.integrations.ai.orchestrator;

public enum OrchestratorStatus {
    EXECUTED,
    AWAITING_CONFIRMATION,
    CLARIFICATION_REQUIRED,
    CANCELLED,
    UNSUPPORTED,
    ERROR
}
