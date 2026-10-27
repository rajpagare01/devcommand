package com.devcommand.devcommand.integrations.ai;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandDispatcher;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.model.ContextualEntity;
import com.devcommand.devcommand.conversation.service.ConversationContextService;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.integrations.ai.confirmation.ConfirmationManager;
import com.devcommand.devcommand.integrations.ai.orchestrator.AiOrchestrator;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorRequest;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorResponse;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorStatus;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.AiToolRegistry;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationResult;
import com.devcommand.devcommand.integrations.gemini.service.NaturalLanguageInterpreter;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiMultiTurnConversationIntegrationTest {

    @Mock private NaturalLanguageInterpreter interpreter;
    @Mock private AiToolRegistry toolRegistry;
    @Mock private CommandDispatcher commandDispatcher;
    @Mock private ConversationContextService contextService;

    private ConfirmationManager confirmationManager;
    private AiOrchestrator orchestrator;

    private final Long userId = 10L;

    // In-memory backing context storage to simulate ConversationContextService state per conversation
    private final Map<String, Map<ContextType, ContextualEntity>> contextStore = new LinkedHashMap<>();

    @BeforeEach
    void setUp() {
        confirmationManager = new ConfirmationManager();
        orchestrator = new AiOrchestrator(interpreter, toolRegistry, confirmationManager, commandDispatcher, contextService);

        contextStore.clear();

        // Wire mock contextService to the in-memory test store
        when(contextService.getEntity(eq(userId), any(), any())).thenAnswer(inv -> {
            String cId = inv.getArgument(1);
            ContextType type = inv.getArgument(2);
            Map<ContextType, ContextualEntity> map = contextStore.get(cId);
            return map != null ? Optional.ofNullable(map.get(type)) : Optional.empty();
        });

        doAnswer(inv -> {
            String cId = inv.getArgument(1);
            List<ContextualEntity> newEntities = inv.getArgument(4);
            Map<ContextType, ContextualEntity> map = contextStore.computeIfAbsent(cId, k -> new LinkedHashMap<>());
            if (newEntities != null) {
                for (ContextualEntity e : newEntities) {
                    map.put(e.entityType(), e);
                }
            }
            return null;
        }).when(contextService).recordTurn(eq(userId), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Conversation 1 — DSA: 'Solved Two Sum' -> 'Revise it' -> 'Make it high priority'")
    void testConversation1_DsaToRevisionTaskToHighPriority() {
        String convId = "conv-1-dsa";

        // Mock Tool: create_dsa_problem
        AiTool dsaTool = mock(AiTool.class);
        when(toolRegistry.getTool("create_dsa_problem")).thenReturn(Optional.of(dsaTool));
        DsaProblemResponse dsaResp = new DsaProblemResponse(100L, "Two Sum", "LEETCODE", null, "Arrays & Hashing", Difficulty.EASY, ProblemStatus.SOLVED, LocalDate.now(), 15, null, null, null, null);
        when(dsaTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Tracked EASY problem 'Two Sum' on LEETCODE.", dsaResp));

        // Mock Tool: create_task
        AiTool createTaskTool = mock(AiTool.class);
        when(toolRegistry.getTool("create_task")).thenReturn(Optional.of(createTaskTool));
        DailyTaskResponse taskResp = new DailyTaskResponse(200L, "Revise Two Sum", null, TaskCategory.PERSONAL, TaskPriority.MEDIUM, DailyTaskStatus.TODO, LocalDate.now().plusDays(1), null, null, null);
        when(createTaskTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Created task 'Revise Two Sum'.", taskResp));

        // Mock Tool: update_task_status
        AiTool updateTaskTool = mock(AiTool.class);
        when(toolRegistry.getTool("update_task_status")).thenReturn(Optional.of(updateTaskTool));
        DailyTaskResponse updatedTaskResp = new DailyTaskResponse(200L, "Revise Two Sum", null, TaskCategory.PERSONAL, TaskPriority.HIGH, DailyTaskStatus.TODO, LocalDate.now().plusDays(1), null, null, null);
        when(updateTaskTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Task 'Revise Two Sum' updated (Priority: HIGH).", updatedTaskResp));

        // --- Turn 1: "Today I solved Two Sum on LeetCode." ---
        Command cmd1 = new Command(CommandType.CREATE_DSA_PROBLEM, userId, new CommandParameters(Map.of("title", "Two Sum", "platform", "LEETCODE", "difficulty", "EASY")));
        when(interpreter.interpret(eq(userId), eq("Today I solved Two Sum on LeetCode."), any())).thenReturn(InterpretationResult.ready(cmd1));

        OrchestratorResponse resp1 = orchestrator.process(new OrchestratorRequest(userId, "Today I solved Two Sum on LeetCode.", convId, null));
        assertThat(resp1.status()).isEqualTo(OrchestratorStatus.EXECUTED);
        assertThat(resp1.responseMessage()).contains("Two Sum");

        // Verify LAST_DSA_PROBLEM was stored in context
        assertThat(contextStore.get(convId)).containsKey(ContextType.LAST_DSA_PROBLEM);
        assertThat(contextStore.get(convId).get(ContextType.LAST_DSA_PROBLEM).title()).isEqualTo("Two Sum");

        // --- Turn 2: "Create a task to revise it tomorrow." ---
        Command cmd2 = new Command(CommandType.CREATE_TASK, userId, new CommandParameters(Map.of("title", "Revise Two Sum", "dueDate", LocalDate.now().plusDays(1).toString())));
        when(interpreter.interpret(eq(userId), eq("Create a task to revise it tomorrow."), any())).thenReturn(InterpretationResult.ready(cmd2));

        OrchestratorResponse resp2 = orchestrator.process(new OrchestratorRequest(userId, "Create a task to revise it tomorrow.", convId, null));
        assertThat(resp2.status()).isEqualTo(OrchestratorStatus.EXECUTED);
        assertThat(resp2.responseMessage()).contains("Revise Two Sum");

        // Verify LAST_TASK was stored in context alongside LAST_DSA_PROBLEM
        assertThat(contextStore.get(convId)).containsKey(ContextType.LAST_TASK);
        assertThat(contextStore.get(convId).get(ContextType.LAST_TASK).title()).isEqualTo("Revise Two Sum");
        assertThat(contextStore.get(convId).get(ContextType.LAST_DSA_PROBLEM).title()).isEqualTo("Two Sum");

        // --- Turn 3: "Make it high priority." ---
        Command cmd3 = new Command(CommandType.UPDATE_TASK, userId, new CommandParameters(Map.of("id", "200", "priority", "HIGH")));
        when(interpreter.interpret(eq(userId), eq("Make it high priority."), any())).thenReturn(InterpretationResult.ready(cmd3));

        OrchestratorResponse resp3 = orchestrator.process(new OrchestratorRequest(userId, "Make it high priority.", convId, null));
        assertThat(resp3.status()).isEqualTo(OrchestratorStatus.EXECUTED);
        assertThat(resp3.responseMessage()).contains("Priority: HIGH");

        // Verify task updated in context
        assertThat(contextStore.get(convId).get(ContextType.LAST_TASK).metadata().get("priority")).isEqualTo("HIGH");
    }

    @Test
    @DisplayName("Conversation 2 — DSA follow-up: 'I solved Binary Search today' -> 'What difficulty was it?' -> EASY")
    void testConversation2_DsaFollowUpDifficulty() {
        String convId = "conv-2-bs";

        // Mock Tool: create_dsa_problem
        AiTool dsaTool = mock(AiTool.class);
        when(toolRegistry.getTool("create_dsa_problem")).thenReturn(Optional.of(dsaTool));
        DsaProblemResponse dsaResp = new DsaProblemResponse(105L, "Binary Search", "LEETCODE", null, "Binary Search", Difficulty.EASY, ProblemStatus.SOLVED, LocalDate.now(), 10, null, null, null, null);
        when(dsaTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Tracked EASY problem 'Binary Search'.", dsaResp));

        // Mock Tool: get_dsa_problem
        AiTool getDsaTool = mock(AiTool.class);
        when(toolRegistry.getTool("get_dsa_problem")).thenReturn(Optional.of(getDsaTool));
        when(getDsaTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Found DSA problem 'Binary Search': Difficulty is EASY.", dsaResp));

        // Turn 1: "I solved Binary Search today."
        Command cmd1 = new Command(CommandType.CREATE_DSA_PROBLEM, userId, new CommandParameters(Map.of("title", "Binary Search", "platform", "LEETCODE", "difficulty", "EASY")));
        when(interpreter.interpret(eq(userId), eq("I solved Binary Search today."), any())).thenReturn(InterpretationResult.ready(cmd1));
        orchestrator.process(new OrchestratorRequest(userId, "I solved Binary Search today.", convId, null));

        // Turn 2: "What difficulty was it?"
        Command cmd2 = new Command(CommandType.READ_DSA_PROBLEM, userId, new CommandParameters(Map.of("id", "105", "title", "Binary Search")));
        when(interpreter.interpret(eq(userId), eq("What difficulty was it?"), any())).thenReturn(InterpretationResult.ready(cmd2));

        OrchestratorResponse resp2 = orchestrator.process(new OrchestratorRequest(userId, "What difficulty was it?", convId, null));
        assertThat(resp2.status()).isEqualTo(OrchestratorStatus.EXECUTED);
        assertThat(resp2.responseMessage()).contains("EASY");
    }

    @Test
    @DisplayName("Conversation 3 — Task: Create task -> 'Make it high priority' updates created task")
    void testConversation3_TaskUpdate() {
        String convId = "conv-3-task";

        AiTool createTaskTool = mock(AiTool.class);
        when(toolRegistry.getTool("create_task")).thenReturn(Optional.of(createTaskTool));
        DailyTaskResponse taskResp = new DailyTaskResponse(300L, "prepare Spring Security interview questions", null, TaskCategory.LEARNING, TaskPriority.MEDIUM, DailyTaskStatus.TODO, null, null, null, null);
        when(createTaskTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Created task.", taskResp));

        AiTool updateTaskTool = mock(AiTool.class);
        when(toolRegistry.getTool("update_task_status")).thenReturn(Optional.of(updateTaskTool));
        DailyTaskResponse updatedResp = new DailyTaskResponse(300L, "prepare Spring Security interview questions", null, TaskCategory.LEARNING, TaskPriority.HIGH, DailyTaskStatus.TODO, null, null, null, null);
        when(updateTaskTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Task updated to HIGH priority.", updatedResp));

        // Turn 1
        Command cmd1 = new Command(CommandType.CREATE_TASK, userId, new CommandParameters(Map.of("title", "prepare Spring Security interview questions")));
        when(interpreter.interpret(eq(userId), eq("Create a task to prepare Spring Security interview questions."), any())).thenReturn(InterpretationResult.ready(cmd1));
        orchestrator.process(new OrchestratorRequest(userId, "Create a task to prepare Spring Security interview questions.", convId, null));

        // Turn 2
        Command cmd2 = new Command(CommandType.UPDATE_TASK, userId, new CommandParameters(Map.of("id", "300", "priority", "HIGH")));
        when(interpreter.interpret(eq(userId), eq("Make it high priority."), any())).thenReturn(InterpretationResult.ready(cmd2));

        OrchestratorResponse resp2 = orchestrator.process(new OrchestratorRequest(userId, "Make it high priority.", convId, null));
        assertThat(resp2.status()).isEqualTo(OrchestratorStatus.EXECUTED);
        assertThat(resp2.responseMessage()).contains("HIGH");
    }

    @Test
    @DisplayName("Conversation 4 — Project: 'Create DevCommand project' -> 'Show me its tasks'")
    void testConversation4_ProjectTasks() {
        String convId = "conv-4-proj";

        AiTool projectTool = mock(AiTool.class);
        when(toolRegistry.getTool("create_project")).thenReturn(Optional.of(projectTool));
        ProjectResponse projResp = new ProjectResponse(400L, "DevCommand", "Developer CLI", null, null, com.devcommand.devcommand.projects.entity.ProjectStatus.IN_PROGRESS, null, null, null, null);
        when(projectTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Project 'DevCommand' created.", projResp));

        AiTool listTasksTool = mock(AiTool.class);
        when(toolRegistry.getTool("list_tasks")).thenReturn(Optional.of(listTasksTool));
        when(listTasksTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Found 3 tasks for project #400.", List.of()));

        // Turn 1
        Command cmd1 = new Command(CommandType.CREATE_PROJECT, userId, new CommandParameters(Map.of("name", "DevCommand")));
        when(interpreter.interpret(eq(userId), eq("Create my DevCommand project."), any())).thenReturn(InterpretationResult.ready(cmd1));
        orchestrator.process(new OrchestratorRequest(userId, "Create my DevCommand project.", convId, null));

        // Turn 2
        Command cmd2 = new Command(CommandType.READ_PENDING_TASKS, userId, new CommandParameters(Map.of("projectId", "400")));
        when(interpreter.interpret(eq(userId), eq("Show me its tasks."), any())).thenReturn(InterpretationResult.ready(cmd2));

        OrchestratorResponse resp2 = orchestrator.process(new OrchestratorRequest(userId, "Show me its tasks.", convId, null));
        assertThat(resp2.status()).isEqualTo(OrchestratorStatus.EXECUTED);
        assertThat(resp2.responseMessage()).contains("Found 3 tasks for project #400");
    }

    @Test
    @DisplayName("Conversation 5 — Job Application: 'Applied to Stripe' -> 'Show me the application'")
    void testConversation5_JobApplication() {
        String convId = "conv-5-job";

        AiTool jobTool = mock(AiTool.class);
        when(toolRegistry.getTool("create_job_application")).thenReturn(Optional.of(jobTool));
        JobApplicationResponse jobResp = new JobApplicationResponse(500L, "Stripe", "Backend Engineer", null, null, null, null, LocalDate.now(), com.devcommand.devcommand.jobs.entity.ApplicationStatus.APPLIED, null, null, null);
        when(jobTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Application to Stripe tracked.", jobResp));

        AiTool listJobsTool = mock(AiTool.class);
        when(toolRegistry.getTool("list_job_applications")).thenReturn(Optional.of(listJobsTool));
        when(listJobsTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Found 1 job application for Stripe.", List.of(jobResp)));

        // Turn 1
        Command cmd1 = new Command(CommandType.CREATE_JOB_APPLICATION, userId, new CommandParameters(Map.of("company", "Stripe", "role", "Backend Engineer", "status", "APPLIED")));
        when(interpreter.interpret(eq(userId), eq("I applied to Stripe for Backend Engineer."), any())).thenReturn(InterpretationResult.ready(cmd1));
        orchestrator.process(new OrchestratorRequest(userId, "I applied to Stripe for Backend Engineer.", convId, null));

        // Turn 2
        Command cmd2 = new Command(CommandType.READ_JOB_PIPELINE, userId, new CommandParameters(Map.of("company", "Stripe")));
        when(interpreter.interpret(eq(userId), eq("Show me the application."), any())).thenReturn(InterpretationResult.ready(cmd2));

        OrchestratorResponse resp2 = orchestrator.process(new OrchestratorRequest(userId, "Show me the application.", convId, null));
        assertThat(resp2.status()).isEqualTo(OrchestratorStatus.EXECUTED);
        assertThat(resp2.responseMessage()).contains("Stripe");
    }

    @Test
    @DisplayName("Conversation 6 — Ambiguity: 'Update it' with both Task and Project asks clarification instead of guessing")
    void testConversation6_AmbiguityResolution() {
        String convId = "conv-6-ambig";

        ContextualEntity taskEntity = ContextualEntity.task(10L, "Revise Two Sum", convId, Map.of());
        ContextualEntity projectEntity = ContextualEntity.project(20L, "DevCommand", convId, Map.of());

        // Prepopulate store with conflicting entities
        contextStore.computeIfAbsent(convId, k -> new LinkedHashMap<>()).put(ContextType.LAST_TASK, taskEntity);
        contextStore.get(convId).put(ContextType.LAST_PROJECT, projectEntity);

        OrchestratorResponse response = orchestrator.process(new OrchestratorRequest(userId, "Update it.", convId, null));

        assertThat(response.status()).isEqualTo(OrchestratorStatus.CLARIFICATION_REQUIRED);
        assertThat(response.responseMessage()).contains("Do you mean the task 'Revise Two Sum' or the project 'DevCommand'?");
    }

    @Test
    @DisplayName("Conversation 7 — Cross conversation: Conversation B cannot access Conversation A's context")
    void testConversation7_CrossConversationIsolation() {
        String convA = "conversation-A";
        String convB = "conversation-B";

        // Conversation A records Two Sum
        contextStore.computeIfAbsent(convA, k -> new LinkedHashMap<>()).put(
                ContextType.LAST_DSA_PROBLEM,
                ContextualEntity.dsa(100L, "Two Sum", "LEETCODE", convA, Map.of("difficulty", "EASY"))
        );

        // In Conversation B: User asks "What problem did I just solve?"
        when(interpreter.interpret(eq(userId), eq("What problem did I just solve?"), any()))
                .thenReturn(InterpretationResult.clarification("Which problem are you referring to?", Map.of()));

        OrchestratorResponse response = orchestrator.process(new OrchestratorRequest(userId, "What problem did I just solve?", convB, null));

        // Conversation B context must remain completely empty and ask for clarification
        assertThat(response.status()).isEqualTo(OrchestratorStatus.CLARIFICATION_REQUIRED);
        assertThat(contextStore.containsKey(convB)).isFalse();
    }

    @Test
    @DisplayName("Security: Failed tool execution does NOT update context entities")
    void testToolFailureDoesNotUpdateContext() {
        String convId = "conv-fail-test";

        AiTool taskTool = mock(AiTool.class);
        when(toolRegistry.getTool("create_task")).thenReturn(Optional.of(taskTool));
        when(taskTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.failure("Database connection failed."));

        Command cmd = new Command(CommandType.CREATE_TASK, userId, new CommandParameters(Map.of("title", "Broken Task")));
        when(interpreter.interpret(eq(userId), eq("Create a task"), any())).thenReturn(InterpretationResult.ready(cmd));

        OrchestratorResponse response = orchestrator.process(new OrchestratorRequest(userId, "Create a task", convId, null));

        // Response shows failure
        assertThat(response.responseMessage()).contains("Action failed: Database connection failed.");
        // Context entity was NOT updated
        assertThat(contextStore.get(convId)).isNullOrEmpty();
    }
}
