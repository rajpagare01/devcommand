package com.devcommand.devcommand.integrations.ai.orchestrator;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandDispatcher;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.ai.confirmation.ConfirmationManager;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.AiToolRegistry;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationResult;
import com.devcommand.devcommand.integrations.gemini.service.NaturalLanguageInterpreter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiOrchestratorTest {

    @Mock private NaturalLanguageInterpreter interpreter;
    @Mock private AiToolRegistry toolRegistry;
    @Mock private CommandDispatcher commandDispatcher;

    private ConfirmationManager confirmationManager;
    private AiOrchestrator orchestrator;

    private final Long userId = 1L;

    @BeforeEach
    void setUp() {
        confirmationManager = new ConfirmationManager();
        orchestrator = new AiOrchestrator(interpreter, toolRegistry, confirmationManager, commandDispatcher);
    }

    @Test
    @DisplayName("Single tool execution flow")
    void testSingleToolExecution() {
        String message = "Today I solved Two Sum on LeetCode";
        Command command = new Command(
                CommandType.CREATE_DSA_PROBLEM,
                userId,
                new CommandParameters(Map.of("title", "Two Sum", "platform", "LEETCODE", "difficulty", "EASY", "status", "SOLVED"))
        );
        when(interpreter.interpret(eq(userId), eq(message), any())).thenReturn(InterpretationResult.ready(command));

        AiTool mockTool = mock(AiTool.class);
        when(mockTool.getName()).thenReturn("create_dsa_problem");
        when(mockTool.requiresConfirmation(any(), eq(userId))).thenReturn(false);
        when(mockTool.execute(any(), eq(userId)))
                .thenReturn(ToolExecutionResult.success("Tracked EASY problem 'Two Sum' on LEETCODE."));

        when(toolRegistry.getTool("create_dsa_problem")).thenReturn(Optional.of(mockTool));

        OrchestratorResponse response = orchestrator.process(new OrchestratorRequest(userId, message));

        assertEquals(OrchestratorStatus.EXECUTED, response.status());
        assertTrue(response.responseMessage().contains("Two Sum"));
        assertEquals(1, response.results().size());
        verify(mockTool).execute(any(), eq(userId));
    }

    @Test
    @DisplayName("Compound action execution: applies for job AND tracks DSA problem")
    void testCompoundActionExecution() {
        String message = "I applied for Backend Engineer at Stripe and solved Two Sum today.";

        Command jobCmd = new Command(
                CommandType.CREATE_JOB_APPLICATION,
                userId,
                new CommandParameters(Map.of("company", "Stripe", "role", "Backend Engineer", "status", "APPLIED"))
        );
        Command dsaCmd = new Command(
                CommandType.CREATE_DSA_PROBLEM,
                userId,
                new CommandParameters(Map.of("title", "Two Sum", "platform", "LEETCODE", "difficulty", "EASY", "status", "SOLVED"))
        );

        when(interpreter.interpret(eq(userId), eq(message), any())).thenReturn(InterpretationResult.ready(List.of(jobCmd, dsaCmd)));

        AiTool jobTool = mock(AiTool.class);
        when(jobTool.getName()).thenReturn("create_job_application");
        when(jobTool.requiresConfirmation(any(), eq(userId))).thenReturn(false);
        when(jobTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Recorded application for Backend Engineer at Stripe."));

        AiTool dsaTool = mock(AiTool.class);
        when(dsaTool.getName()).thenReturn("create_dsa_problem");
        when(dsaTool.requiresConfirmation(any(), eq(userId))).thenReturn(false);
        when(dsaTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Tracked EASY problem 'Two Sum' on LEETCODE."));

        when(toolRegistry.getTool("create_job_application")).thenReturn(Optional.of(jobTool));
        when(toolRegistry.getTool("create_dsa_problem")).thenReturn(Optional.of(dsaTool));

        OrchestratorResponse response = orchestrator.process(new OrchestratorRequest(userId, message));

        assertEquals(OrchestratorStatus.EXECUTED, response.status());
        assertEquals(2, response.results().size());
        assertTrue(response.responseMessage().contains("Stripe"));
        assertTrue(response.responseMessage().contains("Two Sum"));
        verify(jobTool).execute(any(), eq(userId));
        verify(dsaTool).execute(any(), eq(userId));
    }

    @Test
    @DisplayName("Clarification required when domain entity resolution is ambiguous")
    void testClarificationFlow() {
        String message = "Today I solved that problem";
        when(interpreter.interpret(eq(userId), eq(message), any()))
                .thenReturn(InterpretationResult.clarification("Which problem did you solve?", Map.of("action", "CREATE_DSA_PROBLEM")));

        OrchestratorResponse response = orchestrator.process(new OrchestratorRequest(userId, message));

        assertEquals(OrchestratorStatus.CLARIFICATION_REQUIRED, response.status());
        assertEquals("Which problem did you solve?", response.responseMessage());
        assertNotNull(response.pendingContext());
    }

    @Test
    @DisplayName("Safety confirmation request for sensitive actions and subsequent confirmation")
    void testConfirmationAndExecutionFlow() {
        String message = "Mark my Stripe application as REJECTED";
        Command command = new Command(
                CommandType.CREATE_JOB_APPLICATION,
                userId,
                new CommandParameters(Map.of("company", "Stripe", "status", "REJECTED"))
        );
        when(interpreter.interpret(eq(userId), eq(message), any())).thenReturn(InterpretationResult.ready(command));

        AiTool jobTool = mock(AiTool.class);
        when(jobTool.getName()).thenReturn("create_job_application");
        when(jobTool.requiresConfirmation(any(), eq(userId))).thenReturn(true);
        when(jobTool.getConfirmationMessage(any(), eq(userId))).thenReturn("Are you sure you want to mark Stripe as REJECTED?");

        when(toolRegistry.getTool("create_job_application")).thenReturn(Optional.of(jobTool));

        // 1. Initial request -> Awaiting confirmation
        OrchestratorResponse response = orchestrator.process(new OrchestratorRequest(userId, message));

        assertEquals(OrchestratorStatus.AWAITING_CONFIRMATION, response.status());
        assertTrue(response.responseMessage().contains("Are you sure"));
        assertNotNull(response.confirmationToken());

        // 2. User confirms with "yes"
        when(jobTool.execute(any(), eq(userId))).thenReturn(ToolExecutionResult.success("Updated application for Stripe to REJECTED."));
        OrchestratorResponse confirmResponse = orchestrator.process(new OrchestratorRequest(userId, "yes"));

        assertEquals(OrchestratorStatus.EXECUTED, confirmResponse.status());
        assertTrue(confirmResponse.responseMessage().contains("Confirmed"));
        verify(jobTool).execute(any(), eq(userId));
    }

    @Test
    @DisplayName("Cancellation flow removes active pending action")
    void testCancellationFlow() {
        // Create an active pending action
        confirmationManager.createPendingAction(userId, "delete_task", Map.of("id", 1), "Delete task");

        // User says "no"
        OrchestratorResponse cancelResponse = orchestrator.process(new OrchestratorRequest(userId, "cancel"));

        assertEquals(OrchestratorStatus.CANCELLED, cancelResponse.status());
        assertTrue(cancelResponse.responseMessage().contains("cancelled"));
        assertTrue(confirmationManager.getActivePendingAction(userId).isEmpty());
    }
}
