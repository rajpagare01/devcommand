package com.devcommand.devcommand.integrations.ai.tools.dsa;

import com.devcommand.devcommand.dsa.dto.CreateDsaProblemRequest;
import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.entity.Difficulty;
import com.devcommand.devcommand.dsa.entity.ProblemStatus;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateDsaProblemTool implements AiTool {

    private final DsaProblemService dsaProblemService;

    @Override
    public String getName() {
        return "create_dsa_problem";
    }

    @Override
    public String getDescription() {
        return "Creates and tracks a new DSA problem with platform, difficulty, topic, and status.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Creates a DSA problem for tracking",
                ToolParameter.required("title", "string", "Title of the problem"),
                ToolParameter.optional("platform", "string", "Platform name (e.g. LEETCODE, HACKERRANK)"),
                ToolParameter.optional("difficulty", "string", "Difficulty (EASY, MEDIUM, HARD)"),
                ToolParameter.optional("topic", "string", "Topic/tag (e.g. Array, Dynamic Programming)"),
                ToolParameter.optional("status", "string", "Problem status (SOLVED, TODO, REVISION, MASTERED)"),
                ToolParameter.optional("timeTaken", "integer", "Time taken in minutes"),
                ToolParameter.optional("notes", "string", "Notes or solution summary"),
                ToolParameter.optional("problemUrl", "string", "URL to the problem")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String title = (String) parameters.get("title");
        if (title == null || title.isBlank()) {
            return ToolExecutionResult.failure("Problem title is required.");
        }

        String platform = parameters.get("platform") != null ? parameters.get("platform").toString() : "LEETCODE";
        String topic = parameters.get("topic") != null ? parameters.get("topic").toString() : "Uncategorized";
        String problemUrl = parameters.get("problemUrl") != null ? parameters.get("problemUrl").toString() : null;
        String notes = parameters.get("notes") != null ? parameters.get("notes").toString() : null;

        Difficulty difficulty = Difficulty.EASY;
        if (parameters.get("difficulty") != null) {
            try {
                difficulty = Difficulty.valueOf(parameters.get("difficulty").toString().toUpperCase());
            } catch (Exception ignored) {}
        }

        ProblemStatus status = ProblemStatus.SOLVED;
        if (parameters.get("status") != null) {
            try {
                status = ProblemStatus.valueOf(parameters.get("status").toString().toUpperCase());
            } catch (Exception ignored) {}
        }

        Integer timeTaken = null;
        if (parameters.get("timeTaken") != null) {
            try {
                timeTaken = Integer.parseInt(parameters.get("timeTaken").toString());
                if (timeTaken <= 0) timeTaken = null;
            } catch (Exception ignored) {}
        }

        CreateDsaProblemRequest request = new CreateDsaProblemRequest(
                title,
                platform,
                problemUrl,
                topic,
                difficulty,
                status,
                LocalDate.now(),
                timeTaken,
                notes,
                null
        );

        try {
            DsaProblemResponse response = dsaProblemService.create(request, userId);
            String summary = String.format("Tracked %s problem '%s' on %s (Status: %s).",
                    response.difficulty(), response.title(), response.platform(), response.status());
            return ToolExecutionResult.success(summary, response);
        } catch (Exception e) {
            log.error("Failed to create DSA problem: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("Failed to create DSA problem: " + e.getMessage());
        }
    }
}
