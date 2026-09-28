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
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@lombok.extern.slf4j.Slf4j
public class AnalyticsService {

    private final DsaProblemRepository dsaProblemRepository;
    private final DailyTaskRepository dailyTaskRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final InterviewRoundRepository interviewRoundRepository;
    private final LearningTopicRepository learningTopicRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final EntityManager entityManager;

    public AnalyticsOverviewResponse getOverviewAnalytics(Long userId) {
        String sql = """
            SELECT 
                (SELECT COUNT(*) FROM dsa_problems WHERE user_id = :userId) as total_dsa,
                (SELECT COUNT(*) FROM dsa_problems WHERE user_id = :userId AND status = 'SOLVED') as solved_dsa,
                
                (SELECT COUNT(*) FROM daily_tasks WHERE user_id = :userId) as total_tasks,
                (SELECT COUNT(*) FROM daily_tasks WHERE user_id = :userId AND status = 'COMPLETED') as completed_tasks,
                (SELECT COUNT(*) FROM daily_tasks WHERE user_id = :userId AND status IN ('TODO', 'IN_PROGRESS')) as pending_tasks,
                
                (SELECT COUNT(*) FROM job_applications WHERE user_id = :userId) as total_jobs,
                (SELECT COUNT(*) FROM job_applications WHERE user_id = :userId AND status IN ('APPLIED', 'SCREENING', 'INTERVIEW')) as active_jobs,
                
                (SELECT COUNT(*) FROM learning_topics WHERE user_id = :userId) as total_learning,
                (SELECT COUNT(*) FROM learning_topics WHERE user_id = :userId AND status = 'COMPLETED') as completed_learning,
                
                (SELECT COUNT(*) FROM projects WHERE user_id = :userId) as total_projects,
                (SELECT COUNT(*) FROM projects WHERE user_id = :userId AND status = 'IN_PROGRESS') as active_projects,
                (SELECT COUNT(*) FROM projects WHERE user_id = :userId AND status = 'COMPLETED') as completed_projects,
                
                (SELECT COUNT(*) FROM project_tasks pt JOIN projects p ON pt.project_id = p.id WHERE p.user_id = :userId) as total_project_tasks,
                (SELECT COUNT(*) FROM project_tasks pt JOIN projects p ON pt.project_id = p.id WHERE p.user_id = :userId AND pt.status = 'DONE') as completed_project_tasks
            """;

        Object[] row = (Object[]) entityManager.createNativeQuery(sql)
                .setParameter("userId", userId)
                .getSingleResult();

        return AnalyticsOverviewResponse.builder()
                .totalDsaProblems(row[0] != null ? ((Number) row[0]).longValue() : 0L)
                .solvedDsaProblems(row[1] != null ? ((Number) row[1]).longValue() : 0L)
                
                .totalTasks(row[2] != null ? ((Number) row[2]).longValue() : 0L)
                .completedTasks(row[3] != null ? ((Number) row[3]).longValue() : 0L)
                .pendingTasks(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                
                .totalJobApplications(row[5] != null ? ((Number) row[5]).longValue() : 0L)
                .activeJobApplications(row[6] != null ? ((Number) row[6]).longValue() : 0L)
                
                .totalLearningTopics(row[7] != null ? ((Number) row[7]).longValue() : 0L)
                .completedLearningTopics(row[8] != null ? ((Number) row[8]).longValue() : 0L)
                
                .totalProjects(row[9] != null ? ((Number) row[9]).longValue() : 0L)
                .activeProjects(row[10] != null ? ((Number) row[10]).longValue() : 0L)
                .completedProjects(row[11] != null ? ((Number) row[11]).longValue() : 0L)
                
                .totalProjectTasks(row[12] != null ? ((Number) row[12]).longValue() : 0L)
                .completedProjectTasks(row[13] != null ? ((Number) row[13]).longValue() : 0L)
                .build();
    }

    public DsaAnalyticsResponse getDsaAnalytics(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate startOfMonth = today.withDayOfMonth(1);

        DsaProblemRepository.DsaAnalyticsProjection proj = dsaProblemRepository.getDsaAnalyticsByUserId(userId, today, startOfWeek, startOfMonth);

        if (proj == null || proj.getTotal() == null || proj.getTotal() == 0L) {
             return DsaAnalyticsResponse.builder()
                .totalProblems(0L).solvedProblems(0L).pendingProblems(0L).revisionProblems(0L)
                .masteredProblems(0L).easyProblems(0L).mediumProblems(0L).hardProblems(0L)
                .solvedToday(0L).solvedThisWeek(0L).solvedThisMonth(0L).build();
        }

        return DsaAnalyticsResponse.builder()
                .totalProblems(proj.getTotal())
                .solvedProblems(proj.getSolvedCount() != null ? proj.getSolvedCount() : 0L)
                .pendingProblems(proj.getTodoCount() != null ? proj.getTodoCount() : 0L)
                .revisionProblems(proj.getRevisionCount() != null ? proj.getRevisionCount() : 0L)
                .masteredProblems(proj.getMasteredCount() != null ? proj.getMasteredCount() : 0L)
                .easyProblems(proj.getEasyCount() != null ? proj.getEasyCount() : 0L)
                .mediumProblems(proj.getMediumCount() != null ? proj.getMediumCount() : 0L)
                .hardProblems(proj.getHardCount() != null ? proj.getHardCount() : 0L)
                .solvedToday(proj.getSolvedToday() != null ? proj.getSolvedToday() : 0L)
                .solvedThisWeek(proj.getSolvedThisWeek() != null ? proj.getSolvedThisWeek() : 0L)
                .solvedThisMonth(proj.getSolvedThisMonth() != null ? proj.getSolvedThisMonth() : 0L)
                .build();
    }

    public TaskAnalyticsResponse getTaskAnalytics(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        DailyTaskRepository.TasksAnalyticsProjection proj = dailyTaskRepository.getTasksAnalyticsByUserId(userId, today, startOfDay, endOfDay);

        if (proj == null || proj.getTotalTasks() == null || proj.getTotalTasks() == 0L) {
            return TaskAnalyticsResponse.builder()
                .totalTasks(0L)
                .todoTasks(0L)
                .inProgressTasks(0L)
                .completedTasks(0L)
                .tasksDueToday(0L)
                .overdueTasks(0L)
                .completedToday(0L)
                .build();
        }

        return TaskAnalyticsResponse.builder()
                .totalTasks(proj.getTotalTasks())
                .todoTasks(proj.getTodoTasks() != null ? proj.getTodoTasks() : 0L)
                .inProgressTasks(proj.getInProgressTasks() != null ? proj.getInProgressTasks() : 0L)
                .completedTasks(proj.getCompletedTasks() != null ? proj.getCompletedTasks() : 0L)
                .tasksDueToday(proj.getTasksDueToday() != null ? proj.getTasksDueToday() : 0L)
                .overdueTasks(proj.getOverdueTasks() != null ? proj.getOverdueTasks() : 0L)
                .completedToday(proj.getCompletedToday() != null ? proj.getCompletedToday() : 0L)
                .build();
    }

    public JobAnalyticsResponse getJobAnalytics(Long userId) {
        java.util.Map<ApplicationStatus, Long> counts = jobApplicationRepository.countStatusByUserId(userId).stream()
                .collect(java.util.stream.Collectors.toMap(
                        JobApplicationRepository.ApplicationStatusCount::getStatus,
                        JobApplicationRepository.ApplicationStatusCount::getCount
                ));

        return JobAnalyticsResponse.builder()
                .totalApplications(jobApplicationRepository.countByUserId(userId))
                .saved(counts.getOrDefault(ApplicationStatus.SAVED, 0L))
                .applied(counts.getOrDefault(ApplicationStatus.APPLIED, 0L))
                .screening(counts.getOrDefault(ApplicationStatus.SCREENING, 0L))
                .interview(counts.getOrDefault(ApplicationStatus.INTERVIEW, 0L))
                .offer(counts.getOrDefault(ApplicationStatus.OFFER, 0L))
                .rejected(counts.getOrDefault(ApplicationStatus.REJECTED, 0L))
                .withdrawn(counts.getOrDefault(ApplicationStatus.WITHDRAWN, 0L))
                .interviewRounds(interviewRoundRepository.countByJobApplicationUserId(userId))
                .upcomingInterviews(interviewRoundRepository.countByJobApplicationUserIdAndStatus(userId, InterviewStatus.SCHEDULED))
                .completedInterviews(interviewRoundRepository.countByJobApplicationUserIdAndStatus(userId, InterviewStatus.COMPLETED))
                .build();
    }

    public LearningAnalyticsResponse getLearningAnalytics(Long userId) {
        LearningTopicRepository.LearningAnalyticsProjection proj = learningTopicRepository.getLearningAnalyticsByUserId(userId);
        
        List<LearningAnalyticsResponse.TechnologyBreakdown> breakdown = learningTopicRepository.getTechnologyBreakdown(userId)
                .stream()
                .map(stats -> LearningAnalyticsResponse.TechnologyBreakdown.builder()
                        .technology(stats.getTechnology())
                        .topicCount(stats.getTopicCount() != null ? stats.getTopicCount() : 0)
                        .completedCount(stats.getCompletedCount() != null ? stats.getCompletedCount() : 0)
                        .averageProgress(stats.getAverageProgress() != null ? stats.getAverageProgress() : 0.0)
                        .build())
                .toList();

        return LearningAnalyticsResponse.builder()
                .totalTopics(proj != null && proj.getTotal() != null ? proj.getTotal() : 0L)
                .notStarted(proj != null && proj.getNotStarted() != null ? proj.getNotStarted() : 0L)
                .inProgress(proj != null && proj.getInProgress() != null ? proj.getInProgress() : 0L)
                .completed(proj != null && proj.getCompleted() != null ? proj.getCompleted() : 0L)
                .onHold(proj != null && proj.getOnHold() != null ? proj.getOnHold() : 0L)
                .averageProgress(proj != null && proj.getAverageProgress() != null ? proj.getAverageProgress() : 0.0)
                .totalHoursSpent(proj != null && proj.getTotalHoursSpent() != null ? proj.getTotalHoursSpent() : 0.0)
                .technologyBreakdown(breakdown)
                .build();
    }

    public ProjectAnalyticsResponse getProjectAnalytics(Long userId) {
        ProjectRepository.ProjectAnalyticsProjection projProj = projectRepository.getProjectAnalyticsByUserId(userId);
        ProjectTaskRepository.ProjectTaskAnalyticsProjection taskProj = projectTaskRepository.getProjectTaskAnalyticsByUserId(userId);

        long totalTasks = (taskProj != null && taskProj.getTotal() != null) ? taskProj.getTotal() : 0L;
        long completedTasks = (taskProj != null && taskProj.getDone() != null) ? taskProj.getDone() : 0L;
        double completionRate = totalTasks > 0 ? ((double) completedTasks / totalTasks) * 100 : 0.0;

        long totalProjects = (projProj != null && projProj.getTotal() != null) ? projProj.getTotal() : 0L;

        if (totalProjects == 0L && totalTasks == 0L) {
             return ProjectAnalyticsResponse.builder()
                .totalProjects(0L).planning(0L).inProgress(0L).completed(0L).onHold(0L).archived(0L)
                .totalProjectTasks(0L).todoProjectTasks(0L).inProgressProjectTasks(0L).completedProjectTasks(0L)
                .projectTaskCompletionRate(0.0)
                .build();
        }

        return ProjectAnalyticsResponse.builder()
                .totalProjects(totalProjects)
                .planning(projProj != null && projProj.getPlanning() != null ? projProj.getPlanning() : 0L)
                .inProgress(projProj != null && projProj.getInProgress() != null ? projProj.getInProgress() : 0L)
                .completed(projProj != null && projProj.getCompleted() != null ? projProj.getCompleted() : 0L)
                .onHold(projProj != null && projProj.getOnHold() != null ? projProj.getOnHold() : 0L)
                .archived(projProj != null && projProj.getArchived() != null ? projProj.getArchived() : 0L)
                .totalProjectTasks(totalTasks)
                .todoProjectTasks(taskProj != null && taskProj.getTodo() != null ? taskProj.getTodo() : 0L)
                .inProgressProjectTasks(taskProj != null && taskProj.getInProgress() != null ? taskProj.getInProgress() : 0L)
                .completedProjectTasks(completedTasks)
                .projectTaskCompletionRate(completionRate)
                .build();
    }
}
