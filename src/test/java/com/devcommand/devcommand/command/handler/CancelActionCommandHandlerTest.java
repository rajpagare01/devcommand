package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramPendingConfirmationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CancelActionCommandHandlerTest {

    private TelegramPendingConfirmationRepository confirmationRepository;
    private CancelActionCommandHandler handler;

    @BeforeEach
    void setUp() {
        confirmationRepository = mock(TelegramPendingConfirmationRepository.class);
        handler = new CancelActionCommandHandler(confirmationRepository);
    }

    @Test
    void supports() {
        assertTrue(handler.supports(CommandType.CANCEL_ACTION));
    }

    @Test
    void handle_validToken_cancels() {
        Long userId = 1L;
        Long chatId = 12345L;
        String token = "valid-token";
        
        Command command = new Command(CommandType.CANCEL_ACTION, userId, 
                new CommandParameters(Map.of("token", token, "chatId", String.valueOf(chatId))));

        when(confirmationRepository.cancelConfirmation(token, userId, chatId)).thenReturn(1);

        CommandResult result = handler.handle(command);
        
        assertTrue(result.success());
        assertEquals("Action cancelled.", result.message());
    }

    @Test
    void handle_invalidToken_fails() {
        Long userId = 1L;
        Long chatId = 12345L;
        String token = "invalid-token";
        
        Command command = new Command(CommandType.CANCEL_ACTION, userId, 
                new CommandParameters(Map.of("token", token, "chatId", String.valueOf(chatId))));

        when(confirmationRepository.cancelConfirmation(token, userId, chatId)).thenReturn(0);

        CommandResult result = handler.handle(command);
        
        assertFalse(result.success());
        assertEquals("Confirmation token is invalid, belongs to another chat, or has already expired.", result.message());
    }
}
