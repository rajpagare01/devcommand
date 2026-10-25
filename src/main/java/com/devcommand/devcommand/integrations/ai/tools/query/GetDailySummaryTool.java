package com.devcommand.devcommand.integrations.ai.tools.query;

import com.devcommand.devcommand.dsa.repository.DsaProblemRepository;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.jobs.repository.JobApplicationRepository;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.service.ProjectService;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class GetDailySummaryTool implements AiTool {

    private final DailyTaskService dailyTaskService;
    private final DsaProblemRepository dsaProblemRepository;
    private final JobApplicationRepository jobApplicationRepository;
    private final ProjectService projectService;

    @Override
    public String getName() {
        return "get_daily_summary";
    }

    @Override
    public String getDescription() {
        return "Generates an aggregated daily summary including tasks due today, DSA problems solved today, active job applications, and ongoing projects.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of("Fetches a consolidated daily summary across all DevCommand tracking modules");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfMonth = today.withDayOfMonth(1);

        // 1. Tasks
        List<DailyTaskResponse> tasksToday = dailyTaskService.today(userId);
        long completedTasksToday = tasksToday.stream()
                .filter(t -> "COMPLETED".equalsIgnoreCase(t.status().name()))
                .count();

        // 2. DSA
        DsaProblemRepository.DsaAnalyticsProjection dsaStats =
                dsaProblemRepository.getDsaAnalyticsByUserId(userId, today, startOfWeek, startOfMonth);
        long dsaSolvedToday = dsaStats != null && dsaStats.getSolvedToday() != null ? dsaStats.getSolvedToday() : 0;
        long dsaTotal = dsaStats != null && dsaStats.getSolvedCount() != null ? dsaStats.getSolvedCount() : 0;

        // 3. Jobs overview
        JobApplicationRepository.JobsOverviewProjection jobsOverview =
                jobApplicationRepository.getJobsOverviewByUserId(userId);
        long activeJobs = jobsOverview != null && jobsOverview.getActive() != null ? jobsOverview.getActive() : 0;

        // 4. Ongoing Projects
        List<ProjectResponse> activeProjects = projectService.getAll(
                userId, ProjectStatus.IN_PROGRESS, null, 0, 5, "updatedAt", "DESC"
        ).getContent();

        Map<String, Object> summaryData = new LinkedHashMap<>();
        summaryData.put("tasksTodayTotal", tasksToday.size());
        summaryData.put("tasksTodayCompleted", completedTasksToday);
        summaryData.put("tasksToday", tasksToday);
        summaryData.put("dsaSolvedToday", dsaSolvedToday);
        summaryData.put("dsaTotalSolved", dsaTotal);
        summaryData.put("activeJobApplications", activeJobs);
        summaryData.put("ongoingProjectsCount", activeProjects.size());
        summaryData.put("ongoingProjects", activeProjects);

        String summary = String.format(
                "Daily Summary: %d/%d tasks completed today | %d DSA problems solved today (%d total) | %d active job applications | %d ongoing projects.",
                completedTasksToday, tasksToday.size(), dsaSolvedToday, dsaTotal, activeJobs, activeProjects.size()
        );

        return ToolExecutionResult.success(summary, summaryData);
    }
}
