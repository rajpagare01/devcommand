package com.devcommand.devcommand.integrations.ai.tools;

public record ToolExecutionResult(
        boolean success,
        String summary,
        Object data,
        String errorMessage
) {
    public static ToolExecutionResult success(String summary, Object data) {
        return new ToolExecutionResult(true, summary, data, null);
    }

    public static ToolExecutionResult success(String summary) {
        return new ToolExecutionResult(true, summary, null, null);
    }

    public static ToolExecutionResult failure(String errorMessage) {
        return new ToolExecutionResult(false, null, null, errorMessage);
    }
}
