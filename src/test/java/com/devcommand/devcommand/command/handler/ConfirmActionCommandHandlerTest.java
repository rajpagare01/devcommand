package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramPendingConfirmation;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramPendingConfirmationRepository;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ConfirmActionCommandHandlerTest {

    private TelegramPendingConfirmationRepository confirmationRepository;
    private DailyTaskService dailyTaskService;
    private ConfirmActionCommandHandler handler;

    @BeforeEach
    void setUp() {
        confirmationRepository = mock(TelegramPendingConfirmationRepository.class);
        dailyTaskService = mock(DailyTaskService.class);
        handler = new ConfirmActionCommandHandler(confirmationRepository, dailyTaskService);
    }

    @Test
    void supports() {
        assertTrue(handler.supports(CommandType.CONFIRM_ACTION));
    }

    @Test
    void handle_validToken_deletesTask() {
        Long userId = 1L;
        Long chatId = 12345L;
        String token = "valid-token";
        
        Command command = new Command(CommandType.CONFIRM_ACTION, userId, 
                new CommandParameters(Map.of("token", token, "chatId", String.valueOf(chatId))));

        TelegramPendingConfirmation pending = mock(TelegramPendingConfirmation.class);
        when(pending.getActionType()).thenReturn("DELETE_TASK");
        when(pending.getExpiresAt()).thenReturn(LocalDateTime.now().plusMinutes(5));
        when(pending.getTaskId()).thenReturn(99L);
        
        when(confirmationRepository.findValidConfirmation(token, userId, chatId)).thenReturn(Optional.of(pending));
        when(confirmationRepository.consumeConfirmation(eq(token), eq(userId), eq(chatId), eq("DELETE_TASK"), any()))
                .thenReturn(1);

        CommandResult result = handler.handle(command);
        
        assertTrue(result.success());
        assertEquals("Task deleted successfully.", result.message());
        
        verify(dailyTaskService).delete(99L, userId);
    }

    @Test
    void handle_invalidToken_fails() {
        Long userId = 1L;
        Long chatId = 12345L;
        String token = "invalid-token";
        
        Command command = new Command(CommandType.CONFIRM_ACTION, userId, 
                new CommandParameters(Map.of("token", token, "chatId", String.valueOf(chatId))));

        when(confirmationRepository.findValidConfirmation(token, userId, chatId)).thenReturn(Optional.empty());

        CommandResult result = handler.handle(command);
        
        assertFalse(result.success());
        assertEquals("Confirmation token is invalid, belongs to another chat, or has already been consumed.", result.message());
        
        verify(dailyTaskService, never()).delete(anyLong(), anyLong());
    }

    @Test
    void handle_expiredToken_fails() {
        Long userId = 1L;
        Long chatId = 12345L;
        String token = "expired-token";
        
        Command command = new Command(CommandType.CONFIRM_ACTION, userId, 
                new CommandParameters(Map.of("token", token, "chatId", String.valueOf(chatId))));

        TelegramPendingConfirmation pending = mock(TelegramPendingConfirmation.class);
        when(pending.getExpiresAt()).thenReturn(LocalDateTime.now().minusMinutes(1)); // expired
        
        when(confirmationRepository.findValidConfirmation(token, userId, chatId)).thenReturn(Optional.of(pending));

        CommandResult result = handler.handle(command);
        
        assertFalse(result.success());
        assertEquals("Confirmation token has expired.", result.message());
        
        verify(dailyTaskService, never()).delete(anyLong(), anyLong());
    }
}
