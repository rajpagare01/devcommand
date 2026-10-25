package com.devcommand.devcommand.integrations.ai.tools;

import java.util.Map;

public interface AiTool {

    /**
     * Unique programmatic identifier of the tool (e.g. "create_dsa_problem").
     */
    String getName();

    /**
     * Human-readable description of what the tool accomplishes.
     */
    String getDescription();

    /**
     * Schema definition specifying parameter requirements and types.
     */
    ToolSchema getSchema();

    /**
     * Whether this tool only reads data without mutating system state.
     */
    boolean isReadOnly();

    /**
     * Whether executing this tool with the given parameters requires explicit user confirmation.
     */
    default boolean requiresConfirmation(Map<String, Object> parameters, Long userId) {
        return false;
    }

    /**
     * Formats a clear confirmation prompt for the user when confirmation is required.
     */
    default String getConfirmationMessage(Map<String, Object> parameters, Long userId) {
        return "Are you sure you want to execute " + getName() + "?";
    }

    /**
     * Executes the tool safely against existing domain services.
     *
     * @param parameters validated, type-safe parameters
     * @param userId explicit authenticated owner ID
     * @return structured execution result
     */
    ToolExecutionResult execute(Map<String, Object> parameters, Long userId);
}
