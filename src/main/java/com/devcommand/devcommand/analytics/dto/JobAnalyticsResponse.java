package com.devcommand.devcommand.analytics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class JobAnalyticsResponse {
    private long totalApplications;
    private long saved;
    private long applied;
    private long screening;
    private long interview;
    private long offer;
    private long rejected;
    private long withdrawn;
    
    private long interviewRounds;
    private long upcomingInterviews;
    private long completedInterviews;
}
