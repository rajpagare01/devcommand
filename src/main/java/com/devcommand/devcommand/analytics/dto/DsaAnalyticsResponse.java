package com.devcommand.devcommand.analytics.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DsaAnalyticsResponse {
    private long totalProblems;
    private long solvedProblems;
    private long pendingProblems;
    private long revisionProblems;
    private long masteredProblems;
    private long easyProblems;
    private long mediumProblems;
    private long hardProblems;
    private long solvedToday;
    private long solvedThisWeek;
    private long solvedThisMonth;
}
