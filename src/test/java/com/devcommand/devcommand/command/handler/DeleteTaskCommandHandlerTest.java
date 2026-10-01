package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramPendingConfirmation;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramPendingConfirmationRepository;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DeleteTaskCommandHandlerTest {

    private DailyTaskService dailyTaskService;
    private TelegramPendingConfirmationRepository confirmationRepository;
    private UserRepository userRepository;
    private DeleteTaskCommandHandler handler;

    @BeforeEach
    void setUp() {
        dailyTaskService = mock(DailyTaskService.class);
        confirmationRepository = mock(TelegramPendingConfirmationRepository.class);
        userRepository = mock(UserRepository.class);
        handler = new DeleteTaskCommandHandler(dailyTaskService, confirmationRepository, userRepository);
    }

    @Test
    void supports() {
        assertTrue(handler.supports(CommandType.DELETE_TASK));
        assertFalse(handler.supports(CommandType.CREATE_TASK));
    }

    @Test
    void handle_withValidId_createsConfirmation() {
        Long userId = 1L;
        Long chatId = 12345L;
        Long taskId = 99L;
        
        Command command = new Command(CommandType.DELETE_TASK, userId, 
                new CommandParameters(Map.of("taskId", String.valueOf(taskId), "chatId", String.valueOf(chatId))));

        DailyTaskResponse taskResponse = new DailyTaskResponse(taskId, "My Task", null, null, null, null, LocalDate.now(), null, null, null);
        when(dailyTaskService.getById(taskId, userId)).thenReturn(taskResponse);
        
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        CommandResult result = handler.handle(command);
        
        assertTrue(result.success());
        assertTrue(result.message().contains("Are you sure you want to delete task 'My Task'?"));
        assertTrue(result.message().contains("/confirm"));
        
        ArgumentCaptor<TelegramPendingConfirmation> captor = ArgumentCaptor.forClass(TelegramPendingConfirmation.class);
        verify(confirmationRepository).save(captor.capture());
        
        TelegramPendingConfirmation saved = captor.getValue();
        assertEquals(userId, saved.getUser().getId());
        assertEquals(chatId, saved.getChatId());
        assertEquals("DELETE_TASK", saved.getActionType());
        assertEquals(taskId, saved.getTaskId());
    }

    @Test
    void handle_withTitleMultipleMatches_requiresClarification() {
        Long userId = 1L;
        Command command = new Command(CommandType.DELETE_TASK, userId, 
                new CommandParameters(Map.of("title", "Task", "chatId", "12345")));

        DailyTaskResponse t1 = new DailyTaskResponse(1L, "Task 1", null, null, null, null, LocalDate.now(), null, null, null);
        DailyTaskResponse t2 = new DailyTaskResponse(2L, "Task 2", null, null, null, null, LocalDate.now(), null, null, null);
        when(dailyTaskService.searchPendingTasks(userId, "Task")).thenReturn(List.of(t1, t2));

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("Multiple pending tasks match 'Task'. Please specify the task ID.", result.message());
        verify(confirmationRepository, never()).save(any());
    }
    
    @Test
    void handle_withNoMatches_returnsNotFound() {
        Long userId = 1L;
        Command command = new Command(CommandType.DELETE_TASK, userId, 
                new CommandParameters(Map.of("title", "Unknown", "chatId", "12345")));

        when(dailyTaskService.searchPendingTasks(userId, "Unknown")).thenReturn(List.of());

        CommandResult result = handler.handle(command);

        assertFalse(result.success());
        assertEquals("No pending tasks found matching 'Unknown'.", result.message());
        verify(confirmationRepository, never()).save(any());
    }
}
