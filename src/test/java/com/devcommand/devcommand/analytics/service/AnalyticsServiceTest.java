package com.devcommand.devcommand.analytics.service;

import com.devcommand.devcommand.analytics.dto.*;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.jobs.entity.ApplicationStatus;
import com.devcommand.devcommand.jobs.entity.InterviewStatus;
import com.devcommand.devcommand.jobs.repository.InterviewRoundRepository;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.learning.entity.LearningStatus;
import com.devcommand.devcommand.learning.repository.LearningTopicRepository;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import com.devcommand.devcommand.projects.repository.ProjectRepository;
import com.devcommand.devcommand.projects.repository.ProjectTaskRepository;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private DsaProblemRepository dsaProblemRepository;
    @Mock private DailyTaskRepository dailyTaskRepository;
    @Mock private JobApplicationRepository jobApplicationRepository;
    @Mock private InterviewRoundRepository interviewRoundRepository;
    @Mock private LearningTopicRepository learningTopicRepository;
    @Mock private ProjectRepository projectRepository;
    @Mock private ProjectTaskRepository projectTaskRepository;
    @Mock private jakarta.persistence.EntityManager entityManager;
    @Mock private jakarta.persistence.Query query;

    @InjectMocks
    private AnalyticsService analyticsService;

    private final Long USER_ID = 1L;

    @Test
    void getOverviewAnalytics_ShouldReturnAllCounts() {
        // Arrange
        Object[] mockRow = new Object[]{
            10L, 5L, // DSA
            20L, 8L, 12L, // Tasks
            15L, 9L, // Jobs
            30L, 12L, // Learning
            5L, 2L, 1L, // Projects
            25L, 10L // ProjectTasks
        };
        
        when(entityManager.createNativeQuery(any(String.class))).thenReturn(query);
        when(query.setParameter(eq("userId"), eq(USER_ID))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(mockRow);

        // Act
        AnalyticsOverviewResponse response = analyticsService.getOverviewAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalDsaProblems()).isEqualTo(10L);
        assertThat(response.getSolvedDsaProblems()).isEqualTo(5L);
        
        assertThat(response.getTotalTasks()).isEqualTo(20L);
        assertThat(response.getCompletedTasks()).isEqualTo(8L);
        assertThat(response.getPendingTasks()).isEqualTo(12L);
        
        assertThat(response.getTotalJobApplications()).isEqualTo(15L);
        assertThat(response.getActiveJobApplications()).isEqualTo(9L);
        
        assertThat(response.getTotalLearningTopics()).isEqualTo(30L);
        assertThat(response.getCompletedLearningTopics()).isEqualTo(12L);
        
        assertThat(response.getTotalProjects()).isEqualTo(5L);
        assertThat(response.getActiveProjects()).isEqualTo(2L);
        assertThat(response.getCompletedProjects()).isEqualTo(1L);
        
        assertThat(response.getTotalProjectTasks()).isEqualTo(25L);
        assertThat(response.getCompletedProjectTasks()).isEqualTo(10L);
        
        // Verify User Isolation
        verify(entityManager).createNativeQuery(any(String.class));
        verify(query).setParameter("userId", USER_ID);
    }

    @Test
    void getOverviewAnalytics_EmptyRepositories_ShouldReturnZeros() {
        // Arrange
        Object[] mockRow = new Object[]{
            null, null,
            null, null, null,
            null, null,
            null, null,
            null, null, null,
            null, null
        };
        
        when(entityManager.createNativeQuery(any(String.class))).thenReturn(query);
        when(query.setParameter(eq("userId"), eq(USER_ID))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(mockRow);

        // Act
        AnalyticsOverviewResponse response = analyticsService.getOverviewAnalytics(USER_ID);
        
        // Assert
        assertThat(response.getTotalDsaProblems()).isZero();
        assertThat(response.getSolvedDsaProblems()).isZero();
        
        assertThat(response.getTotalTasks()).isZero();
        assertThat(response.getCompletedTasks()).isZero();
        assertThat(response.getPendingTasks()).isZero();
        
        assertThat(response.getTotalJobApplications()).isZero();
        assertThat(response.getActiveJobApplications()).isZero();
        
        assertThat(response.getTotalLearningTopics()).isZero();
        assertThat(response.getCompletedLearningTopics()).isZero();
        
        assertThat(response.getTotalProjects()).isZero();
        assertThat(response.getActiveProjects()).isZero();
        assertThat(response.getCompletedProjects()).isZero();
        
        assertThat(response.getTotalProjectTasks()).isZero();
        assertThat(response.getCompletedProjectTasks()).isZero();
    }

    @Test
    void getDsaAnalytics_ShouldReturnCorrectData() {
        // Arrange
        DsaProblemRepository.DsaAnalyticsProjection mockProj = new DsaProblemRepository.DsaAnalyticsProjection() {
            public Long getTotal() { return 50L; }
            public Long getTodoCount() { return 15L; }
            public Long getSolvedCount() { return 20L; }
            public Long getRevisionCount() { return 10L; }
            public Long getMasteredCount() { return 5L; }
            public Long getEasyCount() { return 25L; }
            public Long getMediumCount() { return 20L; }
            public Long getHardCount() { return 5L; }
            public Long getSolvedToday() { return 2L; }
            public Long getSolvedThisWeek() { return 7L; }
            public Long getSolvedThisMonth() { return 15L; }
        };
        
        when(dsaProblemRepository.getDsaAnalyticsByUserId(eq(USER_ID), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(mockProj);

        // Act
        DsaAnalyticsResponse response = analyticsService.getDsaAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalProblems()).isEqualTo(50L);
        assertThat(response.getSolvedProblems()).isEqualTo(20L);
        assertThat(response.getPendingProblems()).isEqualTo(15L);
        assertThat(response.getRevisionProblems()).isEqualTo(10L);
        assertThat(response.getMasteredProblems()).isEqualTo(5L);
        assertThat(response.getEasyProblems()).isEqualTo(25L);
        assertThat(response.getMediumProblems()).isEqualTo(20L);
        assertThat(response.getHardProblems()).isEqualTo(5L);
        
        // Assert Temporal logic
        assertThat(response.getSolvedToday()).isEqualTo(2L);
        assertThat(response.getSolvedThisWeek()).isEqualTo(7L);
        assertThat(response.getSolvedThisMonth()).isEqualTo(15L);
        
        verify(dsaProblemRepository).getDsaAnalyticsByUserId(eq(USER_ID), eq(LocalDate.now()), any(LocalDate.class), any(LocalDate.class));
    }

    @Test
    void getDsaAnalytics_MissingData_ShouldMapToZero() {
        // Arrange
        DsaProblemRepository.DsaAnalyticsProjection mockProj = new DsaProblemRepository.DsaAnalyticsProjection() {
            public Long getTotal() { return 0L; }
            public Long getTodoCount() { return null; }
            public Long getSolvedCount() { return null; }
            public Long getRevisionCount() { return null; }
            public Long getMasteredCount() { return null; }
            public Long getEasyCount() { return null; }
            public Long getMediumCount() { return null; }
            public Long getHardCount() { return null; }
            public Long getSolvedToday() { return null; }
            public Long getSolvedThisWeek() { return null; }
            public Long getSolvedThisMonth() { return null; }
        };
        
        when(dsaProblemRepository.getDsaAnalyticsByUserId(eq(USER_ID), any(LocalDate.class), any(LocalDate.class), any(LocalDate.class)))
            .thenReturn(mockProj);

        // Act
        DsaAnalyticsResponse response = analyticsService.getDsaAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalProblems()).isZero();
        assertThat(response.getSolvedProblems()).isZero();
        assertThat(response.getPendingProblems()).isZero();
        assertThat(response.getRevisionProblems()).isZero();
        assertThat(response.getMasteredProblems()).isZero();
        assertThat(response.getEasyProblems()).isZero();
        assertThat(response.getMediumProblems()).isZero();
        assertThat(response.getHardProblems()).isZero();
        assertThat(response.getSolvedToday()).isZero();
        assertThat(response.getSolvedThisWeek()).isZero();
        assertThat(response.getSolvedThisMonth()).isZero();
    }

    @Test
    void getTaskAnalytics_ShouldReturnCorrectData() {
        // Arrange
        LocalDate today = LocalDate.now();
        DailyTaskRepository.TasksAnalyticsProjection mockProj = new DailyTaskRepository.TasksAnalyticsProjection() {
            public Long getTotalTasks() { return 100L; }
            public Long getTodoTasks() { return 40L; }
            public Long getInProgressTasks() { return 10L; }
            public Long getCompletedTasks() { return 50L; }
            public Long getTasksDueToday() { return 5L; }
            public Long getOverdueTasks() { return 3L; }
            public Long getCompletedToday() { return 4L; }
        };
        
        when(dailyTaskRepository.getTasksAnalyticsByUserId(
                eq(USER_ID), eq(today), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(mockProj);

        // Act
        TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalTasks()).isEqualTo(100L);
        assertThat(response.getTodoTasks()).isEqualTo(40L);
        assertThat(response.getInProgressTasks()).isEqualTo(10L);
        assertThat(response.getCompletedTasks()).isEqualTo(50L);
        assertThat(response.getTasksDueToday()).isEqualTo(5L);
        
        // Verify overdue calculation ignores COMPLETED
        assertThat(response.getOverdueTasks()).isEqualTo(3L);
        
        // Verify completedToday logic
        assertThat(response.getCompletedToday()).isEqualTo(4L);
    }

    @Test
    void getTaskAnalytics_MissingData_ShouldMapToZero() {
        // Arrange
        LocalDate today = LocalDate.now();
        DailyTaskRepository.TasksAnalyticsProjection mockProj = new DailyTaskRepository.TasksAnalyticsProjection() {
            public Long getTotalTasks() { return 0L; }
            public Long getTodoTasks() { return null; }
            public Long getInProgressTasks() { return null; }
            public Long getCompletedTasks() { return null; }
            public Long getTasksDueToday() { return null; }
            public Long getOverdueTasks() { return null; }
            public Long getCompletedToday() { return null; }
        };
        
        when(dailyTaskRepository.getTasksAnalyticsByUserId(
                eq(USER_ID), eq(today), any(LocalDateTime.class), any(LocalDateTime.class)
        )).thenReturn(mockProj);

        // Act
        TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalTasks()).isZero();
        assertThat(response.getTodoTasks()).isZero();
        assertThat(response.getInProgressTasks()).isZero();
        assertThat(response.getCompletedTasks()).isZero();
        assertThat(response.getTasksDueToday()).isZero();
        assertThat(response.getOverdueTasks()).isZero();
        assertThat(response.getCompletedToday()).isZero();
    }

    @Test
    void getJobAnalytics_ShouldReturnCorrectData() {
        // Arrange
        when(jobApplicationRepository.countByUserId(USER_ID)).thenReturn(40L);
        
        JobApplicationRepository.ApplicationStatusCount savedCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.SAVED; }
            public Long getCount() { return 5L; }
        };
        JobApplicationRepository.ApplicationStatusCount appliedCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.APPLIED; }
            public Long getCount() { return 10L; }
        };
        JobApplicationRepository.ApplicationStatusCount screeningCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.SCREENING; }
            public Long getCount() { return 8L; }
        };
        JobApplicationRepository.ApplicationStatusCount interviewCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.INTERVIEW; }
            public Long getCount() { return 7L; }
        };
        JobApplicationRepository.ApplicationStatusCount offerCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.OFFER; }
            public Long getCount() { return 2L; }
        };
        JobApplicationRepository.ApplicationStatusCount rejectedCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.REJECTED; }
            public Long getCount() { return 5L; }
        };
        JobApplicationRepository.ApplicationStatusCount withdrawnCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.WITHDRAWN; }
            public Long getCount() { return 3L; }
        };
        
        when(jobApplicationRepository.countStatusByUserId(USER_ID)).thenReturn(List.of(
            savedCount, appliedCount, screeningCount, interviewCount, offerCount, rejectedCount, withdrawnCount
        ));

        when(interviewRoundRepository.countByJobApplicationUserId(USER_ID)).thenReturn(12L);
        when(interviewRoundRepository.countByJobApplicationUserIdAndStatus(USER_ID, InterviewStatus.SCHEDULED)).thenReturn(4L);
        when(interviewRoundRepository.countByJobApplicationUserIdAndStatus(USER_ID, InterviewStatus.COMPLETED)).thenReturn(8L);

        // Act
        JobAnalyticsResponse response = analyticsService.getJobAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalApplications()).isEqualTo(40L);
        assertThat(response.getSaved()).isEqualTo(5L);
        assertThat(response.getApplied()).isEqualTo(10L);
        assertThat(response.getScreening()).isEqualTo(8L);
        assertThat(response.getInterview()).isEqualTo(7L);
        assertThat(response.getOffer()).isEqualTo(2L);
        assertThat(response.getRejected()).isEqualTo(5L);
        assertThat(response.getWithdrawn()).isEqualTo(3L);
        
        assertThat(response.getInterviewRounds()).isEqualTo(12L);
        assertThat(response.getUpcomingInterviews()).isEqualTo(4L);
        assertThat(response.getCompletedInterviews()).isEqualTo(8L);
    }

    @Test
    void getJobAnalytics_MissingStatuses_ShouldMapToZero() {
        // Arrange
        when(jobApplicationRepository.countByUserId(USER_ID)).thenReturn(7L);
        
        JobApplicationRepository.ApplicationStatusCount appliedCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.APPLIED; }
            public Long getCount() { return 5L; }
        };
        JobApplicationRepository.ApplicationStatusCount interviewCount = new JobApplicationRepository.ApplicationStatusCount() {
            public ApplicationStatus getStatus() { return ApplicationStatus.INTERVIEW; }
            public Long getCount() { return 2L; }
        };
        
        when(jobApplicationRepository.countStatusByUserId(USER_ID)).thenReturn(List.of(
            appliedCount, interviewCount
        ));

        // Act
        JobAnalyticsResponse response = analyticsService.getJobAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalApplications()).isEqualTo(7L);
        assertThat(response.getSaved()).isZero();
        assertThat(response.getApplied()).isEqualTo(5L);
        assertThat(response.getScreening()).isZero();
        assertThat(response.getInterview()).isEqualTo(2L);
        assertThat(response.getOffer()).isZero();
        assertThat(response.getRejected()).isZero();
        assertThat(response.getWithdrawn()).isZero();
    }

    @Test
    void getLearningAnalytics_WithData_ShouldReturnCorrectMetrics() {
        // Arrange
        LearningTopicRepository.LearningAnalyticsProjection mockProj = new LearningTopicRepository.LearningAnalyticsProjection() {
            public Long getTotal() { return 20L; }
            public Long getNotStarted() { return 5L; }
            public Long getInProgress() { return 7L; }
            public Long getCompleted() { return 6L; }
            public Long getOnHold() { return 2L; }
            public Double getAverageProgress() { return 45.5; }
            public Double getTotalHoursSpent() { return 120.5; }
        };
        when(learningTopicRepository.getLearningAnalyticsByUserId(USER_ID)).thenReturn(mockProj);

        LearningTopicRepository.TechnologyStats stats = new LearningTopicRepository.TechnologyStats() {
            @Override public String getTechnology() { return "Java"; }
            @Override public Long getTopicCount() { return 5L; }
            @Override public Long getCompletedCount() { return 3L; }
            @Override public Double getAverageProgress() { return 60.0; }
        };
        when(learningTopicRepository.getTechnologyBreakdown(USER_ID)).thenReturn(List.of(stats));

        // Act
        LearningAnalyticsResponse response = analyticsService.getLearningAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalTopics()).isEqualTo(20L);
        assertThat(response.getNotStarted()).isEqualTo(5L);
        assertThat(response.getInProgress()).isEqualTo(7L);
        assertThat(response.getCompleted()).isEqualTo(6L);
        assertThat(response.getOnHold()).isEqualTo(2L);
        assertThat(response.getAverageProgress()).isEqualTo(45.5);
        assertThat(response.getTotalHoursSpent()).isEqualTo(120.5);
        
        assertThat(response.getTechnologyBreakdown()).hasSize(1);
        assertThat(response.getTechnologyBreakdown().get(0).getTechnology()).isEqualTo("Java");
        assertThat(response.getTechnologyBreakdown().get(0).getTopicCount()).isEqualTo(5L);
        assertThat(response.getTechnologyBreakdown().get(0).getCompletedCount()).isEqualTo(3L);
        assertThat(response.getTechnologyBreakdown().get(0).getAverageProgress()).isEqualTo(60.0);
    }

    @Test
    void getLearningAnalytics_EmptyData_ShouldMapNullsToZero() {
        // Arrange
        LearningTopicRepository.LearningAnalyticsProjection mockProj = new LearningTopicRepository.LearningAnalyticsProjection() {
            public Long getTotal() { return null; }
            public Long getNotStarted() { return null; }
            public Long getInProgress() { return null; }
            public Long getCompleted() { return null; }
            public Long getOnHold() { return null; }
            public Double getAverageProgress() { return null; }
            public Double getTotalHoursSpent() { return null; }
        };
        when(learningTopicRepository.getLearningAnalyticsByUserId(USER_ID)).thenReturn(mockProj);
        when(learningTopicRepository.getTechnologyBreakdown(USER_ID)).thenReturn(Collections.emptyList());

        // Act
        LearningAnalyticsResponse response = analyticsService.getLearningAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalTopics()).isZero();
        assertThat(response.getNotStarted()).isZero();
        assertThat(response.getInProgress()).isZero();
        assertThat(response.getCompleted()).isZero();
        assertThat(response.getOnHold()).isZero();
        assertThat(response.getAverageProgress()).isEqualTo(0.0);
        assertThat(response.getTotalHoursSpent()).isEqualTo(0.0);
        assertThat(response.getTechnologyBreakdown()).isEmpty();
    }

    @Test
    void getProjectAnalytics_WithData_ShouldCalculateCompletionRate() {
        // Arrange
        ProjectRepository.ProjectAnalyticsProjection mockProj = new ProjectRepository.ProjectAnalyticsProjection() {
            public Long getTotal() { return 10L; }
            public Long getPlanning() { return 2L; }
            public Long getInProgress() { return 3L; }
            public Long getCompleted() { return 4L; }
            public Long getOnHold() { return 1L; }
            public Long getArchived() { return 0L; }
        };
        when(projectRepository.getProjectAnalyticsByUserId(USER_ID)).thenReturn(mockProj);

        ProjectTaskRepository.ProjectTaskAnalyticsProjection mockTask = new ProjectTaskRepository.ProjectTaskAnalyticsProjection() {
            public Long getTotal() { return 100L; }
            public Long getTodo() { return 20L; }
            public Long getInProgress() { return 5L; }
            public Long getDone() { return 75L; }
        };
        when(projectTaskRepository.getProjectTaskAnalyticsByUserId(USER_ID)).thenReturn(mockTask);

        // Act
        ProjectAnalyticsResponse response = analyticsService.getProjectAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalProjects()).isEqualTo(10L);
        assertThat(response.getPlanning()).isEqualTo(2L);
        assertThat(response.getInProgress()).isEqualTo(3L);
        assertThat(response.getCompleted()).isEqualTo(4L);
        assertThat(response.getOnHold()).isEqualTo(1L);
        assertThat(response.getArchived()).isZero();

        assertThat(response.getTotalProjectTasks()).isEqualTo(100L);
        assertThat(response.getTodoProjectTasks()).isEqualTo(20L);
        assertThat(response.getInProgressProjectTasks()).isEqualTo(5L);
        assertThat(response.getCompletedProjectTasks()).isEqualTo(75L);
        
        assertThat(response.getProjectTaskCompletionRate()).isEqualTo(75.0); // 75/100 * 100
    }

    @Test
    void getProjectAnalytics_ZeroTasks_ShouldReturnZeroCompletionRate_AvoidingDivisionByZero() {
        // Arrange
        ProjectRepository.ProjectAnalyticsProjection mockProj = new ProjectRepository.ProjectAnalyticsProjection() {
            public Long getTotal() { return 1L; }
            public Long getPlanning() { return 1L; }
            public Long getInProgress() { return 0L; }
            public Long getCompleted() { return 0L; }
            public Long getOnHold() { return 0L; }
            public Long getArchived() { return 0L; }
        };
        when(projectRepository.getProjectAnalyticsByUserId(USER_ID)).thenReturn(mockProj);

        ProjectTaskRepository.ProjectTaskAnalyticsProjection mockTask = new ProjectTaskRepository.ProjectTaskAnalyticsProjection() {
            public Long getTotal() { return 0L; }
            public Long getTodo() { return 0L; }
            public Long getInProgress() { return 0L; }
            public Long getDone() { return 0L; }
        };
        when(projectTaskRepository.getProjectTaskAnalyticsByUserId(USER_ID)).thenReturn(mockTask);

        // Act
        ProjectAnalyticsResponse response = analyticsService.getProjectAnalytics(USER_ID);

        // Assert
        assertThat(response.getProjectTaskCompletionRate()).isEqualTo(0.0);
    }
    
    @Test
    void getProjectAnalytics_MissingData_ShouldMapToZero() {
        // Arrange
        ProjectRepository.ProjectAnalyticsProjection mockProj = new ProjectRepository.ProjectAnalyticsProjection() {
            public Long getTotal() { return 0L; }
            public Long getPlanning() { return null; }
            public Long getInProgress() { return null; }
            public Long getCompleted() { return null; }
            public Long getOnHold() { return null; }
            public Long getArchived() { return null; }
        };
        when(projectRepository.getProjectAnalyticsByUserId(USER_ID)).thenReturn(mockProj);

        ProjectTaskRepository.ProjectTaskAnalyticsProjection mockTask = new ProjectTaskRepository.ProjectTaskAnalyticsProjection() {
            public Long getTotal() { return 0L; }
            public Long getTodo() { return null; }
            public Long getInProgress() { return null; }
            public Long getDone() { return null; }
        };
        when(projectTaskRepository.getProjectTaskAnalyticsByUserId(USER_ID)).thenReturn(mockTask);

        // Act
        ProjectAnalyticsResponse response = analyticsService.getProjectAnalytics(USER_ID);

        // Assert
        assertThat(response.getTotalProjects()).isZero();
        assertThat(response.getPlanning()).isZero();
        assertThat(response.getInProgress()).isZero();
        assertThat(response.getCompleted()).isZero();
        assertThat(response.getOnHold()).isZero();
        assertThat(response.getArchived()).isZero();

        assertThat(response.getTotalProjectTasks()).isZero();
        assertThat(response.getTodoProjectTasks()).isZero();
        assertThat(response.getInProgressProjectTasks()).isZero();
        assertThat(response.getCompletedProjectTasks()).isZero();
        assertThat(response.getProjectTaskCompletionRate()).isZero();
    }
}
