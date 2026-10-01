package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ListTasksCommandHandlerTest {

    private DailyTaskService dailyTaskService;
    private ListTasksCommandHandler handler;

    @BeforeEach
    void setUp() {
        dailyTaskService = Mockito.mock(DailyTaskService.class);
        handler = new ListTasksCommandHandler(dailyTaskService);
    }

    @Test
    void supports_returnsTrueForReadPendingTasks() {
        assertTrue(handler.supports(CommandType.READ_PENDING_TASKS));
        assertFalse(handler.supports(CommandType.CREATE_TASK));
    }

    @Test
    void handle_whenEmptyTasks_returnsEmptyStateMessage() {
        when(dailyTaskService.getPending(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Command command = new Command(CommandType.READ_PENDING_TASKS, 1L, new CommandParameters(Collections.emptyMap()));
        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertTrue(result.message().contains("no pending tasks"));
    }

    @Test
    void handle_whenTasksExist_returnsFormattedMessage() {
        DailyTaskResponse task = new DailyTaskResponse(
                100L,
                "Test Task",
                "Description",
                TaskCategory.PERSONAL,
                TaskPriority.HIGH,
                DailyTaskStatus.TODO,
                LocalDate.now(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(dailyTaskService.getPending(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Command command = new Command(CommandType.READ_PENDING_TASKS, 1L, new CommandParameters(Collections.emptyMap()));
        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertTrue(result.message().contains("ID: 100"));
        assertTrue(result.message().contains("Test Task"));
        assertTrue(result.message().contains("[HIGH]"));
    }

    @Test
    void handle_whenMoreThan10Tasks_truncatesOutput() {
        List<DailyTaskResponse> tasks = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            tasks.add(new DailyTaskResponse(
                    (long) i,
                    "Task " + i,
                    "Desc",
                    TaskCategory.PERSONAL,
                    TaskPriority.MEDIUM,
                    DailyTaskStatus.TODO,
                    LocalDate.now(),
                    null,
                    LocalDateTime.now(),
                    LocalDateTime.now()
            ));
        }

        when(dailyTaskService.getPending(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(tasks));

        Command command = new Command(CommandType.READ_PENDING_TASKS, 1L, new CommandParameters(Collections.emptyMap()));
        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertTrue(result.message().contains("...and more pending tasks not shown"));
        assertTrue(result.message().contains("Task 9"));
        assertFalse(result.message().contains("Task 10")); // 11th task is truncated
    }

    @Test
    void handle_whenTitleContainsMarkdown_escapesCharacters() {
        DailyTaskResponse task = new DailyTaskResponse(
                1L,
                "*Bold* _Italic_ [Link](url) `code`",
                "Description",
                TaskCategory.PERSONAL,
                TaskPriority.HIGH,
                DailyTaskStatus.TODO,
                LocalDate.now(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(dailyTaskService.getPending(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(task)));

        Command command = new Command(CommandType.READ_PENDING_TASKS, 1L, new CommandParameters(Collections.emptyMap()));
        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        String message = result.message();
        assertTrue(message.contains("\\*Bold\\* \\_Italic\\_ \\[Link](url) \\`code\\`"));
    }
}
