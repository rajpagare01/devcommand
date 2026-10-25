package com.devcommand.devcommand.integrations.ai.tools.projects;

import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ListTasksTool implements AiTool {

    private final DailyTaskService dailyTaskService;

    @Override
    public String getName() {
        return "list_tasks";
    }

    @Override
    public String getDescription() {
        return "Lists tasks with optional filters by category, priority, status, or date filters ('today', 'upcoming').";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Lists tasks",
                ToolParameter.optional("filter", "string", "Convenience filter: 'today' or 'upcoming'"),
                ToolParameter.optional("status", "string", "Filter by status (TODO, IN_PROGRESS, COMPLETED)"),
                ToolParameter.optional("category", "string", "Filter by category (PERSONAL, WORK, LEARNING)"),
                ToolParameter.optional("priority", "string", "Filter by priority (LOW, MEDIUM, HIGH, URGENT)"),
                ToolParameter.optional("size", "integer", "Max results (default 10)")
        );
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String filter = parameters.get("filter") != null ? parameters.get("filter").toString() : null;

        if ("today".equalsIgnoreCase(filter)) {
            List<DailyTaskResponse> tasks = dailyTaskService.today(userId);
            return ToolExecutionResult.success("Retrieved " + tasks.size() + " tasks scheduled for today.", tasks);
        } else if ("upcoming".equalsIgnoreCase(filter)) {
            List<DailyTaskResponse> tasks = dailyTaskService.upcoming(userId);
            return ToolExecutionResult.success("Retrieved " + tasks.size() + " upcoming tasks.", tasks);
        }

        String category = parameters.get("category") != null ? parameters.get("category").toString() : null;
        String priority = parameters.get("priority") != null ? parameters.get("priority").toString() : null;
        String status = parameters.get("status") != null ? parameters.get("status").toString() : null;

        int size = 10;
        if (parameters.get("size") != null) {
            try {
                size = Integer.parseInt(parameters.get("size").toString());
                if (size <= 0) size = 10;
            } catch (Exception ignored) {}
        }

        Page<DailyTaskResponse> page = dailyTaskService.getAll(
                userId, category, priority, status, null, 0, size, "createdAt", "DESC"
        );

        String summary = String.format("Found %d tasks (showing %d).", page.getTotalElements(), page.getNumberOfElements());
        return ToolExecutionResult.success(summary, page.getContent());
    }
}
