package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CompleteTaskCommandHandlerTest {

    private DailyTaskService dailyTaskService;
    private CompleteTaskCommandHandler handler;

    @BeforeEach
    void setUp() {
        dailyTaskService = mock(DailyTaskService.class);
        handler = new CompleteTaskCommandHandler(dailyTaskService);
    }

    @Test
    void supports_completeTask() {
        assertTrue(handler.supports(CommandType.COMPLETE_TASK));
        assertFalse(handler.supports(CommandType.CREATE_TASK));
    }

    @Test
    void handle_withId_completesSuccessfully() {
        Command command = new Command(CommandType.COMPLETE_TASK, 1L, Map.of("id", "100"));
        DailyTaskResponse mockResponse = new DailyTaskResponse(100L, "Some task", null, null, null, null, LocalDate.now(), null, null, null);
        when(dailyTaskService.complete(100L, 1L)).thenReturn(mockResponse);

        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertEquals("Task 'Some task' marked as completed.", result.message());
        verify(dailyTaskService).complete(100L, 1L);
    }

    @Test
    void handle_withId_failsWhenNotFound() {
        Command command = new Command(CommandType.COMPLETE_TASK, 1L, Map.of("id", "999"));
        when(dailyTaskService.complete(999L, 1L)).thenThrow(new ResourceNotFoundException("Not found"));

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("Task not found.", result.message());
    }

    @Test
    void handle_withId_failsWhenInvalidFormat() {
        Command command = new Command(CommandType.COMPLETE_TASK, 1L, Map.of("id", "abc"));

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("Invalid task ID format.", result.message());
    }

    @Test
    void handle_withTitle_completesSuccessfully_whenOneMatch() {
        Command command = new Command(CommandType.COMPLETE_TASK, 1L, Map.of("title", "Spring"));
        DailyTaskResponse mockResponse = new DailyTaskResponse(200L, "Spring Security", null, null, null, null, LocalDate.now(), null, null, null);
        
        when(dailyTaskService.searchPendingTasks(1L, "Spring")).thenReturn(List.of(mockResponse));
        when(dailyTaskService.complete(200L, 1L)).thenReturn(mockResponse);

        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertEquals("Task 'Spring Security' marked as completed.", result.message());
        verify(dailyTaskService).complete(200L, 1L);
    }

    @Test
    void handle_withTitle_fails_whenNoMatches() {
        Command command = new Command(CommandType.COMPLETE_TASK, 1L, Map.of("title", "Missing"));
        when(dailyTaskService.searchPendingTasks(1L, "Missing")).thenReturn(List.of());

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("No pending tasks found matching 'Missing'.", result.message());
    }

    @Test
    void handle_withTitle_fails_whenMultipleMatches() {
        Command command = new Command(CommandType.COMPLETE_TASK, 1L, Map.of("title", "Task"));
        DailyTaskResponse t1 = new DailyTaskResponse(1L, "Task 1", null, null, null, null, LocalDate.now(), null, null, null);
        DailyTaskResponse t2 = new DailyTaskResponse(2L, "Task 2", null, null, null, null, LocalDate.now(), null, null, null);
        
        when(dailyTaskService.searchPendingTasks(1L, "Task")).thenReturn(List.of(t1, t2));

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("Multiple pending tasks match 'Task'. Please be more specific or use the task ID.", result.message());
        verify(dailyTaskService, never()).complete(anyLong(), anyLong());
    }

    @Test
    void handle_fails_whenNoTitleOrId() {
        Command command = new Command(CommandType.COMPLETE_TASK, 1L, Map.of());

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("Please specify the task ID or title to complete.", result.message());
    }
}
