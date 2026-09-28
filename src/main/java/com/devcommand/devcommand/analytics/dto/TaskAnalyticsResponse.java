package com.devcommand.devcommand.analytics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TaskAnalyticsResponse {
    private long totalTasks;
    private long todoTasks;
    private long inProgressTasks;
    private long completedTasks;
    private long tasksDueToday;
    private long overdueTasks;
    private long completedToday;
}
