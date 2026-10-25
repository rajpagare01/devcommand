package com.devcommand.devcommand.integrations.ai.tools.dsa;

import com.devcommand.devcommand.dsa.dto.DsaProblemResponse;
import com.devcommand.devcommand.dsa.service.DsaProblemService;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListDsaProblemsTool implements AiTool {

    private final DsaProblemService dsaProblemService;

    @Override
    public String getName() {
        return "list_dsa_problems";
    }

    @Override
    public String getDescription() {
        return "Lists tracked DSA problems with optional filters for topic, platform, difficulty, and status.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Lists DSA problems with optional filters",
                ToolParameter.optional("topic", "string", "Filter by topic"),
                ToolParameter.optional("platform", "string", "Filter by platform (e.g. LEETCODE)"),
                ToolParameter.optional("difficulty", "string", "Filter by difficulty (EASY, MEDIUM, HARD)"),
                ToolParameter.optional("status", "string", "Filter by status (TODO, SOLVED, REVISION, MASTERED)"),
                ToolParameter.optional("size", "integer", "Max results to return (default 10)")
        );
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String topic = parameters.get("topic") != null ? parameters.get("topic").toString() : null;
        String platform = parameters.get("platform") != null ? parameters.get("platform").toString() : null;
        String difficulty = parameters.get("difficulty") != null ? parameters.get("difficulty").toString() : null;
        String status = parameters.get("status") != null ? parameters.get("status").toString() : null;

        int size = 10;
        if (parameters.get("size") != null) {
            try {
                size = Integer.parseInt(parameters.get("size").toString());
                if (size <= 0) size = 10;
            } catch (Exception ignored) {}
        }

        Page<DsaProblemResponse> page = dsaProblemService.getAll(
                userId, topic, platform, difficulty, status, 0, size, "createdAt", "DESC"
        );

        String summary = String.format("Found %d DSA problems (showing %d).",
                page.getTotalElements(), page.getNumberOfElements());

        return ToolExecutionResult.success(summary, page.getContent());
    }
}
