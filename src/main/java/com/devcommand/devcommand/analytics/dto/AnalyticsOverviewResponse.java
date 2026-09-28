package com.devcommand.devcommand.analytics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AnalyticsOverviewResponse {
    private long totalDsaProblems;
    private long solvedDsaProblems;
    private long totalTasks;
    private long completedTasks;
    private long pendingTasks;
    private long totalJobApplications;
    private long activeJobApplications;
    private long totalLearningTopics;
    private long completedLearningTopics;
    private long totalProjects;
    private long activeProjects;
    private long completedProjects;
    private long totalProjectTasks;
    private long completedProjectTasks;
}
