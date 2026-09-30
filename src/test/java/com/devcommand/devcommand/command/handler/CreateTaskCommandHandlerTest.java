package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandException;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateTaskCommandHandlerTest {

    @Mock
    private DailyTaskService dailyTaskService;

    private CreateTaskCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateTaskCommandHandler(dailyTaskService);
    }

    @Test
    void supports_ReturnsTrueForCreateTask() {
        assertTrue(handler.supports(CommandType.CREATE_TASK));
        assertFalse(handler.supports(CommandType.COMPLETE_TASK));
    }

    @Test
    void handle_DelegatesToServiceWithCorrectMappedValues() {
        Long userId = 99L;
        Map<String, Object> params = new HashMap<>();
        params.put("title", "Learn Command Pattern");
        params.put("description", "Refactor devcommand");
        params.put("category", "LEARNING");
        params.put("priority", "HIGH");
        params.put("status", "IN_PROGRESS");
        params.put("dueDate", "2026-10-01");

        Command command = new Command(CommandType.CREATE_TASK, userId, params);

        DailyTaskResponse fakeResponse = new DailyTaskResponse(
                1L, "Learn Command Pattern", "Refactor devcommand",
                TaskCategory.LEARNING, TaskPriority.HIGH, DailyTaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 10, 1), null, null, null
        );

        when(dailyTaskService.create(any(CreateDailyTaskRequest.class), eq(userId))).thenReturn(fakeResponse);

        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertEquals("Task created successfully", result.message());
        assertEquals(fakeResponse, result.data());

        ArgumentCaptor<CreateDailyTaskRequest> captor = ArgumentCaptor.forClass(CreateDailyTaskRequest.class);
        verify(dailyTaskService).create(captor.capture(), eq(userId));

        CreateDailyTaskRequest captured = captor.getValue();
        assertEquals("Learn Command Pattern", captured.title());
        assertEquals("Refactor devcommand", captured.description());
        assertEquals(TaskCategory.LEARNING, captured.category());
        assertEquals(TaskPriority.HIGH, captured.priority());
        assertEquals(DailyTaskStatus.IN_PROGRESS, captured.status());
        assertEquals(LocalDate.of(2026, 10, 1), captured.dueDate());
    }

    @Test
    void handle_MissingTitle_ThrowsException() {
        Command command = new Command(CommandType.CREATE_TASK, 1L, Map.of("description", "No title here"));
        
        CommandException ex = assertThrows(CommandException.class, () -> handler.handle(command));
        assertEquals("Missing required parameter: title", ex.getMessage());
        
        verifyNoInteractions(dailyTaskService);
    }

    @Test
    void handle_DefaultsAreAppliedWhenEnumsMissing() {
        Long userId = 99L;
        Map<String, Object> params = new HashMap<>();
        params.put("title", "Minimal task");

        Command command = new Command(CommandType.CREATE_TASK, userId, params);
        
        DailyTaskResponse fakeResponse = new DailyTaskResponse(
                1L, "Minimal task", null, TaskCategory.PERSONAL, TaskPriority.MEDIUM, DailyTaskStatus.TODO,
                null, null, null, null
        );
        when(dailyTaskService.create(any(CreateDailyTaskRequest.class), eq(userId))).thenReturn(fakeResponse);

        handler.handle(command);

        ArgumentCaptor<CreateDailyTaskRequest> captor = ArgumentCaptor.forClass(CreateDailyTaskRequest.class);
        verify(dailyTaskService).create(captor.capture(), eq(userId));

        CreateDailyTaskRequest captured = captor.getValue();
        assertEquals(TaskCategory.PERSONAL, captured.category());
        assertEquals(TaskPriority.MEDIUM, captured.priority());
        assertEquals(DailyTaskStatus.TODO, captured.status());
    }
    
    @Test
    void handle_InvalidEnum_ThrowsException() {
        Command command = new Command(CommandType.CREATE_TASK, 1L, Map.of("title", "T", "priority", "SUPER_HIGH"));
        
        CommandException ex = assertThrows(CommandException.class, () -> handler.handle(command));
        assertTrue(ex.getMessage().contains("Invalid parameter"));
        
        verifyNoInteractions(dailyTaskService);
    }
}
