package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;

public record TaskResolution(
        ResolutionStatus status,
        Long taskId,
        String title,
        DailyTaskResponse task,
        String clarificationMessage
) {
    public static TaskResolution resolved(Long taskId, String title, DailyTaskResponse task) {
        return new TaskResolution(ResolutionStatus.RESOLVED, taskId, title, task, null);
    }

    public static TaskResolution ambiguous(String clarificationMessage) {
        return new TaskResolution(ResolutionStatus.AMBIGUOUS, null, null, null, clarificationMessage);
    }

    public static TaskResolution notFound(String clarificationMessage) {
        return new TaskResolution(ResolutionStatus.NOT_FOUND, null, null, null, clarificationMessage);
    }

    public boolean isResolved() {
        return status == ResolutionStatus.RESOLVED;
    }
}
