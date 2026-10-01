package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReadJobPipelineCommandHandlerTest {

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    private ReadJobPipelineCommandHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ReadJobPipelineCommandHandler(jobApplicationRepository);
    }

    @Test
    void supports_ReturnsTrueForReadJobPipeline() {
        assertTrue(handler.supports(CommandType.READ_JOB_PIPELINE));
    }

    @Test
    void handle_ReturnsPipelineSuccessfully() {
        Long userId = 99L;
        Command command = new Command(CommandType.READ_JOB_PIPELINE, userId, new CommandParameters(Collections.emptyMap()));

        JobApplicationRepository.JobsOverviewProjection mockOverview = mock(JobApplicationRepository.JobsOverviewProjection.class);
        when(mockOverview.getTotal()).thenReturn(10L);
        when(mockOverview.getActive()).thenReturn(7L);

        when(jobApplicationRepository.getJobsOverviewByUserId(eq(userId))).thenReturn(mockOverview);
        
        JobApplicationRepository.ApplicationStatusCount mockCount = mock(JobApplicationRepository.ApplicationStatusCount.class);
        when(mockCount.getStatus()).thenReturn(com.devcommand.devcommand.jobs.entity.ApplicationStatus.INTERVIEW);
        when(mockCount.getCount()).thenReturn(2L);
        
        when(jobApplicationRepository.countStatusByUserId(eq(userId))).thenReturn(List.of(mockCount));

        CommandResult result = handler.handle(command);

        assertTrue(result.success());
        assertTrue(result.message().contains("Total tracked: 10"));
        assertTrue(result.message().contains("Active pipeline: 7"));
        assertTrue(result.message().contains("INTERVIEW: 2"));

        verify(jobApplicationRepository).getJobsOverviewByUserId(eq(userId));
    }
}
