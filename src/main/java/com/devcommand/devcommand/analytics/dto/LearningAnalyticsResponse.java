package com.devcommand.devcommand.analytics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class LearningAnalyticsResponse {
    private long totalTopics;
    private long notStarted;
    private long inProgress;
    private long completed;
    private long onHold;
    private double averageProgress;
    private double totalHoursSpent;
    private List<TechnologyBreakdown> technologyBreakdown;
    
    @Getter
    @Setter
    @Builder
    public static class TechnologyBreakdown {
        private String technology;
        private long topicCount;
        private long completedCount;
        private double averageProgress;
    }
}
