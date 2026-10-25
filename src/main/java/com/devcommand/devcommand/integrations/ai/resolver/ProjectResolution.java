package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.projects.dto.ProjectResponse;

public record ProjectResolution(
        ResolutionStatus status,
        Long projectId,
        String title,
        ProjectResponse project,
        String clarificationMessage
) {
    public static ProjectResolution resolved(Long projectId, String title, ProjectResponse project) {
        return new ProjectResolution(ResolutionStatus.RESOLVED, projectId, title, project, null);
    }

    public static ProjectResolution ambiguous(String clarificationMessage) {
        return new ProjectResolution(ResolutionStatus.AMBIGUOUS, null, null, null, clarificationMessage);
    }

    public static ProjectResolution notFound(String clarificationMessage) {
        return new ProjectResolution(ResolutionStatus.NOT_FOUND, null, null, null, clarificationMessage);
    }

    public boolean isResolved() {
        return status == ResolutionStatus.RESOLVED;
    }
}
