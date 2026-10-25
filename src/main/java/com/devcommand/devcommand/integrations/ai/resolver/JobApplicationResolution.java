package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.jobs.dto.JobApplicationResponse;

public record JobApplicationResolution(
        ResolutionStatus status,
        Long applicationId,
        String company,
        JobApplicationResponse application,
        String clarificationMessage
) {
    public static JobApplicationResolution resolved(Long applicationId, String company, JobApplicationResponse application) {
        return new JobApplicationResolution(ResolutionStatus.RESOLVED, applicationId, company, application, null);
    }

    public static JobApplicationResolution ambiguous(String clarificationMessage) {
        return new JobApplicationResolution(ResolutionStatus.AMBIGUOUS, null, null, null, clarificationMessage);
    }

    public static JobApplicationResolution notFound(String clarificationMessage) {
        return new JobApplicationResolution(ResolutionStatus.NOT_FOUND, null, null, null, clarificationMessage);
    }

    public boolean isResolved() {
        return status == ResolutionStatus.RESOLVED;
    }
}
