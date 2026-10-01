package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandException;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreateDsaProblemCommandHandlerTest {

    @Mock
    private DsaProblemService dsaProblemService;

    private CreateDsaProblemCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateDsaProblemCommandHandler(dsaProblemService);
    }

    @Test
    void supports_ReturnsTrueForCreateDsaProblem() {
        assertTrue(handler.supports(CommandType.CREATE_DSA_PROBLEM));
        assertFalse(handler.supports(CommandType.CREATE_TASK));
    }

    @Test
    void handle_DelegatesToServiceWithCorrectMappedValues() {
        Long userId = 99L;
        Map<String, Object> params = new HashMap<>();
        params.put("title", "Two Sum");
        params.put("platform", "LeetCode");
        params.put("topic", "Arrays");
        params.put("difficulty", "EASY");
        params.put("status", "SOLVED");
        params.put("timeTaken", 15);

        Command command = new Command(CommandType.CREATE_DSA_PROBLEM, userId, new CommandParameters(params));

        DsaProblemResponse fakeResponse = mock(DsaProblemResponse.class);

        when(dsaProblemService.create(any(CreateDsaProblemRequest.class), eq(userId))).thenReturn(fakeResponse);

        CommandResult result = handler.handle(command);

        assertTrue(result.success(), "Result was not successful: " + result.message());
        assertEquals("Tracked EASY problem 'Two Sum' on LeetCode!", result.message());

        ArgumentCaptor<CreateDsaProblemRequest> captor = ArgumentCaptor.forClass(CreateDsaProblemRequest.class);
        verify(dsaProblemService).create(captor.capture(), eq(userId));

        CreateDsaProblemRequest captured = captor.getValue();
        assertEquals("Two Sum", captured.title());
        assertEquals("LeetCode", captured.platform());
        assertEquals("Arrays", captured.topic());
        assertEquals(Difficulty.EASY, captured.difficulty());
        assertEquals(ProblemStatus.SOLVED, captured.status());
        assertEquals(15, captured.timeTaken());
    }

    @Test
    void handle_NegativeTimeTaken_ReturnsFailure() {
        Command command = new Command(CommandType.CREATE_DSA_PROBLEM, 1L, new CommandParameters(Map.of(
            "title", "Two Sum", "platform", "LeetCode", "topic", "Arrays", "timeTaken", -5
        )));
        
        CommandResult result = handler.handle(command);
        assertFalse(result.success());
        assertEquals("Time taken cannot be negative.", result.message());
        
        verifyNoInteractions(dsaProblemService);
    }

    @Test
    void handle_MissingRequiredFields_ThrowsException() {
        Command command = new Command(CommandType.CREATE_DSA_PROBLEM, 1L, new CommandParameters(Map.of("platform", "LeetCode")));
        
        CommandException ex = assertThrows(CommandException.class, () -> handler.handle(command));
        assertEquals("Missing required parameter: title", ex.getMessage());
        
        verifyNoInteractions(dsaProblemService);
    }
}
