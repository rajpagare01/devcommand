package com.devcommand.devcommand.command;

import com.devcommand.devcommand.command.handler.CommandHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

    @Mock
    private CommandHandler mockHandler;

    private CommandDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher(List.of(mockHandler));
    }

    @Test
    void dispatch_RoutesToCorrectHandler() {
        when(mockHandler.supports(CommandType.CREATE_TASK)).thenReturn(true);
        CommandResult expectedResult = CommandResult.success("Test");
        when(mockHandler.handle(any(Command.class))).thenReturn(expectedResult);

        Command command = new Command(CommandType.CREATE_TASK, 1L, Map.of());
        CommandResult result = dispatcher.dispatch(command);

        assertEquals(expectedResult, result);
        verify(mockHandler).handle(command);
    }

    @Test
    void dispatch_UnsupportedCommand_ReturnsFailure() {
        when(mockHandler.supports(CommandType.COMPLETE_TASK)).thenReturn(false);

        Command command = new Command(CommandType.COMPLETE_TASK, 1L, Map.of());
        
        CommandResult result = dispatcher.dispatch(command);
        assertFalse(result.success());
        assertTrue(result.message().contains("Unsupported command"));
        verify(mockHandler, never()).handle(any(Command.class));
    }

    @Test
    void dispatch_NullCommand_ThrowsException() {
        assertThrows(CommandException.class, () -> dispatcher.dispatch(null));
    }
}
