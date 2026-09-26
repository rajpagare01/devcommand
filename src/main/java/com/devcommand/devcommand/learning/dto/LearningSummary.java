package com.devcommand.devcommand.learning.dto;

/**
 * Not exposed by any endpoint yet - no /api/analytics route was built, per
 * the spec's explicit instruction. This exists only as a small, clean
 * service-layer aggregate (LearningTopicService.getSummary(userId)) that a
 * future dashboard/analytics module can call directly instead of
 * re-deriving these numbers itself. No new table or persisted analytics
 * data - it's computed on the fly from the caller's existing topics.
 */
public record LearningSummary(
        long totalTopics,
        long completedTopics,
        long inProgressTopics,
        double totalHoursSpent,
        double averageProgress
) {
}
