package com.devcommand.devcommand.integrations.ai.tools;

import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.DsaProblem;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.mapper.DsaProblemMapper;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import com.devcommand.devcommand.integrations.ai.tools.dsa.*;
import com.devcommand.devcommand.integrations.ai.tools.jobs.*;
import com.devcommand.devcommand.integrations.ai.tools.projects.*;
import com.devcommand.devcommand.integrations.ai.tools.query.*;
import com.devcommand.devcommand.jobs.dto.CreateJobApplicationRequest;
import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;
import com.devcommand.devcommand.jobs.dto.JobStatusUpdateRequest;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.entity.JobApplication;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.jobs.service.JobApplicationService;
import com.devcommand.devcommand.projects.dto.ProjectRequest;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.service.ProjectService;
import com.devcommand.devcommand.projects.service.ProjectTaskService;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.mapper.DailyTaskMapper;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiToolsTest {

    @Mock private DsaProblemService dsaProblemService;
    @Mock private DsaProblemRepository dsaProblemRepository;
    @Mock private DsaProblemMapper dsaProblemMapper;

    @Mock private JobApplicationService jobApplicationService;
    @Mock private JobApplicationRepository jobApplicationRepository;

    @Mock private ProjectService projectService;
    @Mock private ProjectTaskService projectTaskService;
    @Mock private DailyTaskService dailyTaskService;
    @Mock private DailyTaskRepository dailyTaskRepository;
    @Mock private DailyTaskMapper dailyTaskMapper;

    private Long userId = 1L;

    @Test
    @DisplayName("CreateDsaProblemTool executes successfully")
    void testCreateDsaProblemTool() {
        CreateDsaProblemTool tool = new CreateDsaProblemTool(dsaProblemService);
        assertEquals("create_dsa_problem", tool.getName());
        assertFalse(tool.isReadOnly());

        DsaProblemResponse mockResp = new DsaProblemResponse(
                10L, "Two Sum", "LEETCODE", "https://leetcode.com", "Array",
                Difficulty.EASY, ProblemStatus.SOLVED, LocalDate.now(), 15, "hashmap", null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(dsaProblemService.create(any(CreateDsaProblemRequest.class), eq(userId))).thenReturn(mockResp);

        ToolExecutionResult result = tool.execute(Map.of("title", "Two Sum", "platform", "LEETCODE", "difficulty", "EASY"), userId);
        assertTrue(result.success());
        assertTrue(result.summary().contains("Two Sum"));
    }

    @Test
    @DisplayName("GetDsaStatsTool executes and formats statistics")
    void testGetDsaStatsTool() {
        GetDsaStatsTool tool = new GetDsaStatsTool(dsaProblemRepository);
        assertTrue(tool.isReadOnly());

        DsaProblemRepository.DsaAnalyticsProjection mockStats = mock(DsaProblemRepository.DsaAnalyticsProjection.class);
        when(mockStats.getTotal()).thenReturn(20L);
        when(mockStats.getSolvedCount()).thenReturn(15L);
        when(mockStats.getEasyCount()).thenReturn(10L);
        when(mockStats.getMediumCount()).thenReturn(4L);
        when(mockStats.getHardCount()).thenReturn(1L);
        when(mockStats.getSolvedToday()).thenReturn(2L);
        when(mockStats.getSolvedThisWeek()).thenReturn(5L);

        when(dsaProblemRepository.getDsaAnalyticsByUserId(eq(userId), any(), any(), any())).thenReturn(mockStats);

        ToolExecutionResult result = tool.execute(Map.of(), userId);
        assertTrue(result.success());
        assertTrue(result.summary().contains("15 solved"));
    }

    @Test
    @DisplayName("CreateJobApplicationTool executes and tracks application")
    void testCreateJobApplicationTool() {
        CreateJobApplicationTool tool = new CreateJobApplicationTool(jobApplicationService);
        assertFalse(tool.isReadOnly());

        JobApplicationResponse mockResp = new JobApplicationResponse(
                5L, "Stripe", "Backend Engineer", "Remote", "https://stripe.com",
                "Referral", "$180k", LocalDate.now(), ApplicationStatus.APPLIED, "notes",
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(jobApplicationService.create(any(CreateJobApplicationRequest.class), eq(userId))).thenReturn(mockResp);

        ToolExecutionResult result = tool.execute(Map.of("company", "Stripe", "role", "Backend Engineer"), userId);
        assertTrue(result.success());
        assertTrue(result.summary().contains("Stripe"));
    }

    @Test
    @DisplayName("UpdateJobApplicationStatusTool requires confirmation on REJECTED status")
    void testUpdateJobStatusConfirmation() {
        UpdateJobApplicationStatusTool tool = new UpdateJobApplicationStatusTool(jobApplicationService, jobApplicationRepository);

        assertTrue(tool.requiresConfirmation(Map.of("status", "REJECTED"), userId));
        assertTrue(tool.requiresConfirmation(Map.of("status", "WITHDRAWN"), userId));
        assertFalse(tool.requiresConfirmation(Map.of("status", "INTERVIEW"), userId));
    }

    @Test
    @DisplayName("CreateProjectTool creates project successfully")
    void testCreateProjectTool() {
        CreateProjectTool tool = new CreateProjectTool(projectService);
        assertFalse(tool.isReadOnly());

        ProjectResponse mockResp = new ProjectResponse(
                2L, "DevCommand AI", "Orchestrator layer", "https://github.com", null,
                ProjectStatus.IN_PROGRESS, LocalDate.now(), null, LocalDateTime.now(), LocalDateTime.now()
        );
        when(projectService.create(any(ProjectRequest.class), eq(userId))).thenReturn(mockResp);

        ToolExecutionResult result = tool.execute(Map.of("name", "DevCommand AI"), userId);
        assertTrue(result.success());
        assertTrue(result.summary().contains("DevCommand AI"));
    }

    @Test
    @DisplayName("CreateTaskTool creates task successfully")
    void testCreateTaskTool() {
        CreateTaskTool tool = new CreateTaskTool(dailyTaskService, projectTaskService);
        assertFalse(tool.isReadOnly());

        DailyTaskResponse mockResp = new DailyTaskResponse(
                8L, "Deploy AI Orchestrator", "Production launch", TaskCategory.PROJECT,
                TaskPriority.HIGH, DailyTaskStatus.TODO, LocalDate.now(), null,
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(dailyTaskService.create(any(CreateDailyTaskRequest.class), eq(userId))).thenReturn(mockResp);

        ToolExecutionResult result = tool.execute(Map.of("title", "Deploy AI Orchestrator", "priority", "HIGH"), userId);
        assertTrue(result.success());
        assertTrue(result.summary().contains("Deploy AI Orchestrator"));
    }

    @Test
    @DisplayName("UpdateTaskStatusTool marks task completed")
    void testUpdateTaskStatusTool() {
        UpdateTaskStatusTool tool = new UpdateTaskStatusTool(dailyTaskService, dailyTaskRepository);

        DailyTask mockTask = new DailyTask();
        mockTask.setId(12L);
        mockTask.setTitle("System audit");
        when(dailyTaskRepository.findByIdAndUserId(12L, userId)).thenReturn(Optional.of(mockTask));

        DailyTaskResponse mockResp = new DailyTaskResponse(
                12L, "System audit", null, TaskCategory.PROJECT, TaskPriority.MEDIUM,
                DailyTaskStatus.COMPLETED, LocalDate.now(), LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now()
        );
        when(dailyTaskService.complete(12L, userId)).thenReturn(mockResp);

        ToolExecutionResult result = tool.execute(Map.of("id", 12L, "status", "COMPLETED"), userId);
        assertTrue(result.success());
        assertTrue(result.summary().contains("COMPLETED"));
    }

    @Test
    @DisplayName("GetDailySummaryTool consolidates tasks, DSA, and jobs")
    void testGetDailySummaryTool() {
        GetDailySummaryTool tool = new GetDailySummaryTool(dailyTaskService, dsaProblemRepository, jobApplicationRepository, projectService);
        assertTrue(tool.isReadOnly());

        when(dailyTaskService.today(userId)).thenReturn(List.of());

        DsaProblemRepository.DsaAnalyticsProjection mockDsa = mock(DsaProblemRepository.DsaAnalyticsProjection.class);
        when(mockDsa.getSolvedToday()).thenReturn(3L);
        when(mockDsa.getSolvedCount()).thenReturn(45L);
        when(dsaProblemRepository.getDsaAnalyticsByUserId(eq(userId), any(), any(), any())).thenReturn(mockDsa);

        JobApplicationRepository.JobsOverviewProjection mockJobs = mock(JobApplicationRepository.JobsOverviewProjection.class);
        when(mockJobs.getActive()).thenReturn(4L);
        when(jobApplicationRepository.getJobsOverviewByUserId(userId)).thenReturn(mockJobs);

        when(projectService.getAll(eq(userId), eq(ProjectStatus.IN_PROGRESS), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(new PageImpl<>(List.of()));

        ToolExecutionResult result = tool.execute(Map.of(), userId);
        assertTrue(result.success());
        assertTrue(result.summary().contains("3 DSA problems solved today"));
        assertTrue(result.summary().contains("4 active job applications"));
    }

    @Test
    @DisplayName("AiToolRegistry correctly registers and finds tools")
    void testAiToolRegistry() {
        CreateDsaProblemTool dsaTool = new CreateDsaProblemTool(dsaProblemService);
        CreateTaskTool taskTool = new CreateTaskTool(dailyTaskService, projectTaskService);

        AiToolRegistry registry = new AiToolRegistry(List.of(dsaTool, taskTool));

        assertTrue(registry.hasTool("create_dsa_problem"));
        assertTrue(registry.hasTool("CREATE_DSA_PROBLEM"));
        assertTrue(registry.hasTool("create_task"));
        assertFalse(registry.hasTool("unknown_tool"));

        assertEquals(2, registry.getAllTools().size());
    }
}
