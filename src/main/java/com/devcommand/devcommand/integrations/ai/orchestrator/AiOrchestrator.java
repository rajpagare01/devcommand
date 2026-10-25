package com.devcommand.devcommand.integrations.ai.orchestrator;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandDispatcher;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.ai.confirmation.ConfirmationManager;
import com.devcommand.devcommand.integrations.ai.confirmation.PendingAction;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.AiToolRegistry;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationResult;
import com.devcommand.devcommand.integrations.gemini.service.NaturalLanguageInterpreter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestrator {

    private static final Set<String> AFFIRMATIVE_RESPONSES = Set.of(
            "yes", "y", "confirm", "proceed", "sure", "ok", "yep", "do it", "approved", "go ahead"
    );

    private static final Set<String> NEGATIVE_RESPONSES = Set.of(
            "no", "n", "cancel", "abort", "stop", "don't", "dont", "never mind", "nevermind", "deny"
    );

    private final NaturalLanguageInterpreter interpreter;
    private final AiToolRegistry toolRegistry;
    private final ConfirmationManager confirmationManager;
    private final CommandDispatcher commandDispatcher;

    /**
     * Central entrypoint coordinating natural-language interpretation, entity resolution,
     * tool selection, safety checks / confirmation, and structured execution.
     */
    public OrchestratorResponse process(OrchestratorRequest request) {
        if (request == null || request.userId() == null) {
            return OrchestratorResponse.error("Authenticated user ID is required.");
        }
        if (request.message() == null || request.message().isBlank()) {
            return OrchestratorResponse.error("Message cannot be empty.");
        }

        Long userId = request.userId();
        String message = request.message().trim();
        String lowerMessage = message.toLowerCase();

        // 1. Check for Active Confirmation or Cancellation Flow
        if (isAffirmative(lowerMessage)) {
            String token = extractToken(message);
            Optional<PendingAction> pendingOpt = token != null 
                    ? confirmationManager.consume(token, userId)
                    : confirmationManager.consumeActive(userId);

            if (pendingOpt.isPresent()) {
                PendingAction pending = pendingOpt.get();
                log.info("Executing confirmed action '{}' for user {}", pending.toolName(), userId);
                return executeConfirmedAction(pending, userId);
            }
        } else if (isNegative(lowerMessage)) {
            boolean cancelled = confirmationManager.cancelActive(userId);
            if (cancelled) {
                return OrchestratorResponse.cancelled("Pending action cancelled.");
            }
        }

        // 2. Natural Language Interpretation & Entity Resolution
        InterpretationResult interpretation;
        try {
            interpretation = interpreter.interpret(userId, message, request.conversationContext());
        } catch (Exception e) {
            log.error("AI interpretation failure for user {}: {}", userId, e.getMessage(), e);
            return OrchestratorResponse.error("I'm having trouble processing that right now. Please try again.");
        }

        switch (interpretation.status()) {
            case CLARIFICATION_REQUIRED:
                return OrchestratorResponse.clarification(interpretation.message(), interpretation.pendingContext());
            case UNSUPPORTED:
                return OrchestratorResponse.unsupported("I cannot handle that request yet. Try asking about tasks, DSA problems, or job applications!");
            case INVALID_MODEL_RESPONSE:
                return OrchestratorResponse.error("I didn't quite understand that. Could you please rephrase?");
            case READY:
                break;
            default:
                return OrchestratorResponse.error("Unexpected interpretation status: " + interpretation.status());
        }

        // 3. Collect Commands (supports compound / multi-step actions)
        List<Command> commands = interpretation.commands();
        if (commands == null || commands.isEmpty()) {
            if (interpretation.command() != null) {
                commands = List.of(interpretation.command());
            } else {
                return OrchestratorResponse.error("No actionable plan could be formed.");
            }
        }

        // 4. Check Safety / Confirmation Requirements for all planned actions
        for (Command command : commands) {
            String toolName = mapCommandToToolName(command.type());
            Optional<AiTool> toolOpt = toolRegistry.getTool(toolName);
            Map<String, Object> params = new LinkedHashMap<>(command.parameters().asMap());

            boolean requiresConfirmation = false;
            String confirmMessage = null;

            if (toolOpt.isPresent()) {
                AiTool tool = toolOpt.get();
                requiresConfirmation = tool.requiresConfirmation(params, userId);
                confirmMessage = tool.getConfirmationMessage(params, userId);
            } else if (command.type() == CommandType.DELETE_TASK) {
                requiresConfirmation = true;
                confirmMessage = "Are you sure you want to delete task '" + params.getOrDefault("title", params.get("taskId")) + "'?";
            }

            if (requiresConfirmation) {
                PendingAction pending = confirmationManager.createPendingAction(
                        userId,
                        toolName != null ? toolName : command.type().name(),
                        params,
                        confirmMessage
                );
                return OrchestratorResponse.awaitingConfirmation(
                        confirmMessage + " Please reply 'yes' to confirm or 'no' to cancel.",
                        pending.confirmationId(),
                        Map.of("tool", pending.toolName(), "parameters", params)
                );
            }
        }

        // 5. Execute Tool(s)
        List<ToolExecutionResult> results = new ArrayList<>();
        for (Command command : commands) {
            ToolExecutionResult execResult = executeCommandThroughToolsOrDispatcher(command, userId);
            results.add(execResult);
        }

        // 6. Format Cohesive Natural Language Response
        String responseSummary = formatCohesiveResponse(results);
        return OrchestratorResponse.executed(responseSummary, results);
    }

    private OrchestratorResponse executeConfirmedAction(PendingAction pending, Long userId) {
        Optional<AiTool> toolOpt = toolRegistry.getTool(pending.toolName());
        if (toolOpt.isPresent()) {
            ToolExecutionResult result = toolOpt.get().execute(pending.parameters(), userId);
            if (result.success()) {
                return OrchestratorResponse.executed("Confirmed. " + result.summary(), result);
            } else {
                return OrchestratorResponse.error("Failed to execute confirmed action: " + result.errorMessage());
            }
        }

        // Fallback for non-tool commands (e.g. DELETE_TASK via CommandDispatcher)
        try {
            CommandType type = CommandType.valueOf(pending.toolName());
            Command command = new Command(type, userId, new com.devcommand.devcommand.command.CommandParameters(pending.parameters()));
            CommandResult result = commandDispatcher.dispatch(command);
            if (result.success()) {
                ToolExecutionResult toolResult = ToolExecutionResult.success(result.message(), result.data());
                return OrchestratorResponse.executed("Confirmed. " + result.message(), toolResult);
            } else {
                return OrchestratorResponse.error("Execution failed: " + result.message());
            }
        } catch (Exception e) {
            log.error("Failed to execute confirmed action: {}", e.getMessage(), e);
            return OrchestratorResponse.error("Failed to execute confirmed action.");
        }
    }

    private ToolExecutionResult executeCommandThroughToolsOrDispatcher(Command command, Long userId) {
        String toolName = mapCommandToToolName(command.type());
        Optional<AiTool> toolOpt = toolRegistry.getTool(toolName);

        if (toolOpt.isPresent()) {
            AiTool tool = toolOpt.get();
            Map<String, Object> params = new LinkedHashMap<>(command.parameters().asMap());

            // Normalize parameters if needed (e.g., COMPLETE_TASK maps to status='COMPLETED')
            if (command.type() == CommandType.COMPLETE_TASK) {
                params.put("status", "COMPLETED");
            }

            return tool.execute(params, userId);
        }

        // Direct dispatcher fallback for commands without dedicated AiTool implementation
        CommandResult result = commandDispatcher.dispatch(command);
        if (result.success()) {
            return ToolExecutionResult.success(result.message(), result.data());
        } else {
            return ToolExecutionResult.failure(result.message());
        }
    }

    private String mapCommandToToolName(CommandType type) {
        if (type == null) return null;
        return switch (type) {
            case CREATE_DSA_PROBLEM -> "create_dsa_problem";
            case CREATE_JOB_APPLICATION -> "create_job_application";
            case CREATE_TASK -> "create_task";
            case COMPLETE_TASK -> "update_task_status";
            case READ_DSA_STATS -> "get_dsa_stats";
            case READ_JOB_PIPELINE -> "list_job_applications";
            case READ_PENDING_TASKS -> "list_tasks";
            case READ_TASKS_TODAY -> "list_tasks";
            default -> null;
        };
    }

    private String formatCohesiveResponse(List<ToolExecutionResult> results) {
        if (results.isEmpty()) {
            return "No actions were performed.";
        }
        if (results.size() == 1) {
            ToolExecutionResult single = results.get(0);
            return single.success() ? single.summary() : ("Action failed: " + single.errorMessage());
        }

        // Multi-action compound response synthesis
        long successCount = results.stream().filter(ToolExecutionResult::success).count();
        if (successCount == results.size()) {
            StringBuilder sb = new StringBuilder("Successfully executed multiple actions:\n");
            for (int i = 0; i < results.size(); i++) {
                sb.append(i + 1).append(". ").append(results.get(i).summary()).append("\n");
            }
            return sb.toString().trim();
        } else {
            StringBuilder sb = new StringBuilder("Completed with some issues:\n");
            for (int i = 0; i < results.size(); i++) {
                ToolExecutionResult res = results.get(i);
                sb.append(i + 1).append(". ")
                  .append(res.success() ? res.summary() : ("Failed: " + res.errorMessage()))
                  .append("\n");
            }
            return sb.toString().trim();
        }
    }

    private boolean isAffirmative(String lower) {
        if (AFFIRMATIVE_RESPONSES.contains(lower)) return true;
        return lower.startsWith("yes") || lower.startsWith("/confirm");
    }

    private boolean isNegative(String lower) {
        if (NEGATIVE_RESPONSES.contains(lower)) return true;
        return lower.startsWith("no") || lower.startsWith("/cancel");
    }

    private String extractToken(String message) {
        if (message.startsWith("/confirm ") || message.startsWith("confirm ")) {
            String[] parts = message.split("\\s+");
            if (parts.length >= 2 && !parts[1].isBlank()) {
                return parts[1].trim();
            }
        }
        return null;
    }
}
