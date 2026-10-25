package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.learning.dto.LearningTopicResponse;

public record LearningTopicResolution(
        ResolutionStatus status,
        Long topicId,
        String topic,
        LearningTopicResponse topicResponse,
        String clarificationMessage
) {
    public static LearningTopicResolution resolved(Long topicId, String topic, LearningTopicResponse topicResponse) {
        return new LearningTopicResolution(ResolutionStatus.RESOLVED, topicId, topic, topicResponse, null);
    }

    public static LearningTopicResolution ambiguous(String clarificationMessage) {
        return new LearningTopicResolution(ResolutionStatus.AMBIGUOUS, null, null, null, clarificationMessage);
    }

    public static LearningTopicResolution notFound(String clarificationMessage) {
        return new LearningTopicResolution(ResolutionStatus.NOT_FOUND, null, null, null, clarificationMessage);
    }

    public boolean isResolved() {
        return status == ResolutionStatus.RESOLVED;
    }
}
