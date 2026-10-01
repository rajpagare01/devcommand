package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandException;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.jobs.dto.CreateJobApplicationRequest;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.service.JobApplicationService;
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
class CreateJobApplicationCommandHandlerTest {

    @Mock
    private JobApplicationService jobApplicationService;

    private CreateJobApplicationCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new CreateJobApplicationCommandHandler(jobApplicationService);
    }

    @Test
    void supports_ReturnsTrueForCreateJobApplication() {
        assertTrue(handler.supports(CommandType.CREATE_JOB_APPLICATION));
    }

    @Test
    void handle_DelegatesToServiceWithCorrectMappedValues() {
        Long userId = 99L;
        Map<String, Object> params = new HashMap<>();
        params.put("company", "Google");
        params.put("role", "Software Engineer");
        params.put("status", "INTERVIEW");

        Command command = new Command(CommandType.CREATE_JOB_APPLICATION, userId, new CommandParameters(params));

        JobApplicationResponse fakeResponse = mock(JobApplicationResponse.class);

        when(jobApplicationService.create(any(CreateJobApplicationRequest.class), eq(userId))).thenReturn(fakeResponse);

        CommandResult result = handler.handle(command);

        assertTrue(result.success(), "Result was not successful: " + result.message());
        assertEquals("Tracked application for Software Engineer at Google (INTERVIEW)!", result.message());

        ArgumentCaptor<CreateJobApplicationRequest> captor = ArgumentCaptor.forClass(CreateJobApplicationRequest.class);
        verify(jobApplicationService).create(captor.capture(), eq(userId));

        CreateJobApplicationRequest captured = captor.getValue();
        assertEquals("Google", captured.company());
        assertEquals("Software Engineer", captured.role());
        assertEquals(ApplicationStatus.INTERVIEW, captured.status());
    }

    @Test
    void handle_MissingCompany_ThrowsException() {
        Command command = new Command(CommandType.CREATE_JOB_APPLICATION, 1L, new CommandParameters(Map.of("role", "Developer")));
        
        CommandException ex = assertThrows(CommandException.class, () -> handler.handle(command));
        assertEquals("Missing required parameter: company", ex.getMessage());
        
        verifyNoInteractions(jobApplicationService);
    }
}
