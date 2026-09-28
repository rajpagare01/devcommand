package com.devcommand.devcommand.analytics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProjectAnalyticsResponse {
    private long totalProjects;
    private long planning;
    private long inProgress;
    private long completed;
    private long onHold;
    private long archived;
    
    private long totalProjectTasks;
    private long todoProjectTasks;
    private long inProgressProjectTasks;
    private long completedProjectTasks;
    
    private double projectTaskCompletionRate;
}
