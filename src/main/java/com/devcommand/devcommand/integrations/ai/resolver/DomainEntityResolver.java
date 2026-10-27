package com.devcommand.devcommand.integrations.ai.resolver;

import com.devcommand.devcommand.dsa.entity.Difficulty;

public interface DomainEntityResolver {

    /**
     * Resolves natural language DSA problem references into authoritative metadata.
     * Checks authoritative DSA catalog first, falls back to explicit user values,
     * or requests clarification when ambiguous or unknown without difficulty.
     */
    DsaProblemResolution resolveDsaProblem(Long userId, String rawTitle, String rawPlatform, Difficulty explicitDifficulty, String chatId);

    /**
     * Backward-compatible overload without user/chat context.
     */
    default DsaProblemResolution resolveDsaProblem(String rawTitle, String rawPlatform, Difficulty explicitDifficulty) {
        return resolveDsaProblem(null, rawTitle, rawPlatform, explicitDifficulty, null);
    }

    /**
     * Resolves task references (pronouns like "it", "that task", or titles) scoped strictly to authenticated userId.
     */
    TaskResolution resolveTask(Long userId, String rawReference, String chatId);

    /**
     * Resolves project references scoped to authenticated userId.
     */
    ProjectResolution resolveProject(Long userId, String rawReference, String chatId);

    /**
     * Resolves job application references scoped to authenticated userId.
     */
    JobApplicationResolution resolveJobApplication(Long userId, String rawReference, String chatId);

    /**
     * Resolves learning topic references scoped to authenticated userId.
     */
    LearningTopicResolution resolveLearningTopic(Long userId, String rawReference, String chatId);
}
