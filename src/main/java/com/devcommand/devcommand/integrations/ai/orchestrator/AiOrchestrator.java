package com.devcommand.devcommand.integrations.ai.orchestrator;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandDispatcher;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.model.ContextualEntity;
import com.devcommand.devcommand.conversation.service.ConversationContextService;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.integrations.ai.confirmation.ConfirmationManager;
import com.devcommand.devcommand.integrations.ai.confirmation.PendingAction;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.AiToolRegistry;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationResult;
import com.devcommand.devcommand.integrations.gemini.service.NaturalLanguageInterpreter;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.dto.ProjectTaskResponse;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
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
    private final ConversationContextService contextService;

    @org.springframework.beans.factory.annotation.Autowired
    public AiOrchestrator(
            NaturalLanguageInterpreter interpreter,
            AiToolRegistry toolRegistry,
            ConfirmationManager confirmationManager,
            CommandDispatcher commandDispatcher,
            ConversationContextService contextService
    ) {
        this.interpreter = interpreter;
        this.toolRegistry = toolRegistry;
        this.confirmationManager = confirmationManager;
        this.commandDispatcher = commandDispatcher;
        this.contextService = contextService;
    }

    public AiOrchestrator(
            NaturalLanguageInterpreter interpreter,
            AiToolRegistry toolRegistry,
            ConfirmationManager confirmationManager,
            CommandDispatcher commandDispatcher
    ) {
        this(interpreter, toolRegistry, confirmationManager, commandDispatcher, null);
    }

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
        String conversationId = request.conversationId();
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
                return executeConfirmedAction(pending, userId, conversationId);
            }
        } else if (isNegative(lowerMessage)) {
            boolean cancelled = confirmationManager.cancelActive(userId);
            if (cancelled) {
                return OrchestratorResponse.cancelled("Pending action cancelled.");
            }
        }

        // 2. Ambiguity Detection: "update it" when multiple conflicting entities exist
        String cleanMessage = lowerMessage.replaceAll("[.?!]+$", "").trim();
        if (contextService != null && (cleanMessage.equals("update it") || cleanMessage.equals("update that") || cleanMessage.equals("modify it"))) {
            Optional<ContextualEntity> taskOpt = contextService.getEntity(userId, conversationId, ContextType.LAST_TASK);
            Optional<ContextualEntity> projectOpt = contextService.getEntity(userId, conversationId, ContextType.LAST_PROJECT);
            if (taskOpt.isPresent() && projectOpt.isPresent()) {
                log.info("Ambiguous update detected between task and project for user {} in conversation {}", userId, conversationId);
                String clarification = String.format("Do you mean the task '%s' or the project '%s'?",
                        taskOpt.get().title(), projectOpt.get().title());
                return OrchestratorResponse.clarification(clarification, conversationId, Map.of(
                        "taskCandidate", taskOpt.get().title(),
                        "projectCandidate", projectOpt.get().title()
                ));
            }
        }

        // 3. Build Conversation Context for the Interpreter
        String memoryContext = "";
        if (contextService != null) {
            String formatted = contextService.formatContextForPrompt(userId, conversationId);
            if (formatted != null) {
                memoryContext = formatted;
            }
        }
        String combinedContext = memoryContext;
        if (request.conversationContext() != null && !request.conversationContext().isBlank()) {
            combinedContext = request.conversationContext() + (memoryContext.isBlank() ? "" : "\n" + memoryContext);
        }

        // 4. Natural Language Interpretation & Entity Resolution
        InterpretationResult interpretation;
        try {
            interpretation = interpreter.interpret(userId, message, combinedContext);
        } catch (Exception e) {
            log.error("AI interpretation failure for user {}: {}", userId, e.getMessage(), e);
            return OrchestratorResponse.error("I'm having trouble processing that right now. Please try again.");
        }

        if (interpretation == null) {
            log.warn("AI interpretation returned null for user {} message '{}'", userId, message);
            return OrchestratorResponse.error("I didn't quite understand that. Could you please rephrase?");
        }

        switch (interpretation.status()) {
            case CLARIFICATION_REQUIRED:
                return OrchestratorResponse.clarification(interpretation.message(), conversationId, interpretation.pendingContext());
            case UNSUPPORTED:
                return OrchestratorResponse.unsupported("I cannot handle that request yet. Try asking about tasks, DSA problems, or job applications!");
            case INVALID_MODEL_RESPONSE:
                return OrchestratorResponse.error("I didn't quite understand that. Could you please rephrase?");
            case READY:
                break;
            default:
                return OrchestratorResponse.error("Unexpected interpretation status: " + interpretation.status());
        }

        // 5. Collect Commands (supports compound / multi-step actions)
        List<Command> commands = interpretation.commands();
        if (commands == null || commands.isEmpty()) {
            if (interpretation.command() != null) {
                commands = List.of(interpretation.command());
            } else {
                return OrchestratorResponse.error("No actionable plan could be formed.");
            }
        }

        // 6. Check Safety / Confirmation Requirements for all planned actions
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

        // 7. Execute Tool(s)
        List<ToolExecutionResult> results = new ArrayList<>();
        List<ContextualEntity> newEntities = new ArrayList<>();

        for (Command command : commands) {
            ToolExecutionResult execResult = executeCommandThroughToolsOrDispatcher(command, userId);
            results.add(execResult);

            if (execResult.success() && execResult.data() != null) {
                extractContextualEntity(command.type(), execResult.data(), conversationId)
                        .ifPresent(newEntities::add);
            }
        }

        // 8. Format Natural Language Response & Persist Memory
        String responseSummary = formatCohesiveResponse(results);

        // Only record turn and entities if at least one action succeeded
        if (contextService != null) {
            boolean anySuccess = results.stream().anyMatch(ToolExecutionResult::success);
            if (anySuccess) {
                contextService.recordTurn(userId, conversationId, message, responseSummary, newEntities);
            } else {
                // Failed execution: do not update entities, only keep message history
                contextService.recordTurn(userId, conversationId, message, responseSummary, List.of());
            }
        }

        return OrchestratorResponse.executed(responseSummary, conversationId, results);
    }

    private OrchestratorResponse executeConfirmedAction(PendingAction pending, Long userId, String conversationId) {
        Optional<AiTool> toolOpt = toolRegistry.getTool(pending.toolName());
        ToolExecutionResult result;

        if (toolOpt.isPresent()) {
            result = toolOpt.get().execute(pending.parameters(), userId);
        } else {
            try {
                CommandType type = CommandType.valueOf(pending.toolName());
                Command command = new Command(type, userId, new com.devcommand.devcommand.command.CommandParameters(pending.parameters()));
                CommandResult cmdResult = commandDispatcher.dispatch(command);
                result = cmdResult.success()
                        ? ToolExecutionResult.success(cmdResult.message(), cmdResult.data())
                        : ToolExecutionResult.failure(cmdResult.message());
            } catch (Exception e) {
                log.error("Failed to execute confirmed action: {}", e.getMessage(), e);
                return OrchestratorResponse.error("Failed to execute confirmed action.");
            }
        }

        if (result.success()) {
            List<ContextualEntity> entities = new ArrayList<>();
            try {
                CommandType type = CommandType.valueOf(pending.toolName());
                extractContextualEntity(type, result.data(), conversationId).ifPresent(entities::add);
            } catch (Exception ignored) {}

            if (contextService != null) {
                contextService.recordTurn(userId, conversationId, "Confirmed", result.summary(), entities);
            }
            return OrchestratorResponse.executed("Confirmed. " + result.summary(), conversationId, List.of(result));
        } else {
            return OrchestratorResponse.error("Failed to execute confirmed action: " + result.errorMessage());
        }
    }

    private Optional<ContextualEntity> extractContextualEntity(CommandType type, Object data, String conversationId) {
        if (data == null) {
            return Optional.empty();
        }

        if (data instanceof DsaProblemResponse dsa) {
            return Optional.of(ContextualEntity.dsa(
                    dsa.id(),
                    dsa.title(),
                    dsa.platform(),
                    conversationId,
                    Map.of("difficulty", dsa.difficulty() != null ? dsa.difficulty().name() : "EASY",
                           "topic", dsa.topic() != null ? dsa.topic() : "Uncategorized")
            ));
        }

        if (data instanceof DailyTaskResponse task) {
            return Optional.of(ContextualEntity.task(
                    task.id(),
                    task.title(),
                    conversationId,
                    Map.of("priority", task.priority() != null ? task.priority().name() : "MEDIUM",
                           "status", task.status() != null ? task.status().name() : "TODO")
            ));
        }

        if (data instanceof ProjectTaskResponse pTask) {
            return Optional.of(ContextualEntity.task(
                    pTask.id(),
                    pTask.title(),
                    conversationId,
                    Map.of("priority", pTask.priority() != null ? pTask.priority().name() : "MEDIUM",
                           "status", pTask.status() != null ? pTask.status().name() : "TODO")
            ));
        }

        if (data instanceof JobApplicationResponse job) {
            return Optional.of(ContextualEntity.job(
                    job.id(),
                    job.company(),
                    conversationId,
                    Map.of("role", job.role() != null ? job.role() : "",
                           "status", job.status() != null ? job.status().name() : "APPLIED")
            ));
        }

        if (data instanceof ProjectResponse proj) {
            return Optional.of(ContextualEntity.project(
                    proj.id(),
                    proj.name(),
                    conversationId,
                    Map.of()
            ));
        }

        return Optional.empty();
    }

    private ToolExecutionResult executeCommandThroughToolsOrDispatcher(Command command, Long userId) {
        String toolName = mapCommandToToolName(command.type());
        Optional<AiTool> toolOpt = toolRegistry.getTool(toolName);

        if (toolOpt.isPresent()) {
            AiTool tool = toolOpt.get();
            Map<String, Object> params = new LinkedHashMap<>(command.parameters().asMap());

            // Normalize parameters if needed
            if (command.type() == CommandType.COMPLETE_TASK) {
                params.put("status", "COMPLETED");
            }

            return tool.execute(params, userId);
        }

        // Direct dispatcher fallback
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
            case READ_DSA_PROBLEM -> "get_dsa_problem";
            case CREATE_PROJECT -> "create_project";
            case CREATE_JOB_APPLICATION -> "create_job_application";
            case CREATE_TASK -> "create_task";
            case UPDATE_TASK -> "update_task_status";
            case COMPLETE_TASK -> "update_task_status";
            case READ_DSA_STATS -> "get_dsa_stats";
            case READ_JOB_PIPELINE -> "list_job_applications";
            case READ_PENDING_TASKS -> "list_tasks";
            case READ_TASKS_TODAY -> "list_tasks";
            case SEARCH_KNOWLEDGE -> "search_knowledge";
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
