package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.dsa.entity.Difficulty;

public record DsaProblemResolution(
        ResolutionStatus status,
        String title,
        String platform,
        Difficulty difficulty,
        String topic,
        String problemUrl,
        String clarificationMessage
) {
    public static DsaProblemResolution resolved(String title, String platform, Difficulty difficulty, String topic, String problemUrl) {
        return new DsaProblemResolution(ResolutionStatus.RESOLVED, title, platform, difficulty, topic, problemUrl, null);
    }

    public static DsaProblemResolution ambiguous(String clarificationMessage) {
        return new DsaProblemResolution(ResolutionStatus.AMBIGUOUS, null, null, null, null, null, clarificationMessage);
    }

    public static DsaProblemResolution unknown(String title, String clarificationMessage) {
        return new DsaProblemResolution(ResolutionStatus.UNKNOWN, title, null, null, null, null, clarificationMessage);
    }

    public boolean isResolved() {
        return status == ResolutionStatus.RESOLVED;
    }
}
