package com.devcommand.devcommand.analytics.controller;

import com.devcommand.devcommand.analytics.dto.*;
import com.devcommand.devcommand.analytics.service.AnalyticsService;
import com.devcommand.devcommand.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    public ResponseEntity<AnalyticsOverviewResponse> getOverviewAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analyticsService.getOverviewAnalytics(principal.getId()));
    }

    @GetMapping("/dsa")
    public ResponseEntity<DsaAnalyticsResponse> getDsaAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analyticsService.getDsaAnalytics(principal.getId()));
    }

    @GetMapping("/tasks")
    public ResponseEntity<TaskAnalyticsResponse> getTaskAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analyticsService.getTaskAnalytics(principal.getId()));
    }

    @GetMapping("/jobs")
    public ResponseEntity<JobAnalyticsResponse> getJobAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analyticsService.getJobAnalytics(principal.getId()));
    }

    @GetMapping("/learning")
    public ResponseEntity<LearningAnalyticsResponse> getLearningAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analyticsService.getLearningAnalytics(principal.getId()));
    }

    @GetMapping("/projects")
    public ResponseEntity<ProjectAnalyticsResponse> getProjectAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(analyticsService.getProjectAnalytics(principal.getId()));
    }
}
