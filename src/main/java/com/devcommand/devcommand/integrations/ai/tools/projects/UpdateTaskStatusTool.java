package com.devcommand.devcommand.integrations.ai.tools.projects;

import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.dto.UpdateDailyTaskRequest;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class UpdateTaskStatusTool implements AiTool {

    private final DailyTaskService dailyTaskService;
    private final DailyTaskRepository dailyTaskRepository;

    @Override
    public String getName() {
        return "update_task_status";
    }

    @Override
    public String getDescription() {
        return "Updates the status of a task (e.g. COMPLETED, IN_PROGRESS, TODO).";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Updates status of a daily task",
                ToolParameter.optional("id", "integer", "ID of the task"),
                ToolParameter.optional("title", "string", "Title of the task if ID is not known"),
                ToolParameter.required("status", "string", "New status (COMPLETED, IN_PROGRESS, TODO)")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String statusStr = parameters.get("status") != null ? parameters.get("status").toString().toUpperCase() : "COMPLETED";

        Long id = null;
        if (parameters.get("id") != null) {
            try {
                id = Long.parseLong(parameters.get("id").toString());
            } catch (Exception ignored) {}
        }

        String title = parameters.get("title") != null ? parameters.get("title").toString() : null;
        Optional<DailyTask> taskOpt;
        if (id != null) {
            taskOpt = dailyTaskRepository.findByIdAndUserId(id, userId);
        } else if (title != null && !title.isBlank()) {
            taskOpt = dailyTaskRepository.findFirstByUserIdAndTitleIgnoreCase(userId, title);
        } else {
            return ToolExecutionResult.failure("Either task ID or title is required.");
        }

        if (taskOpt.isEmpty()) {
            return ToolExecutionResult.failure("Task not found.");
        }

        DailyTask task = taskOpt.get();
        Long taskId = task.getId();

        try {
            DailyTaskResponse response;
            if ("COMPLETED".equalsIgnoreCase(statusStr)) {
                response = dailyTaskService.complete(taskId, userId);
            } else if ("IN_PROGRESS".equalsIgnoreCase(statusStr)) {
                response = dailyTaskService.start(taskId, userId);
            } else {
                DailyTaskStatus targetStatus = DailyTaskStatus.valueOf(statusStr);
                UpdateDailyTaskRequest req = new UpdateDailyTaskRequest(
                        task.getTitle(),
                        task.getDescription(),
                        task.getCategory(),
                        task.getPriority(),
                        targetStatus,
                        task.getDueDate()
                );
                response = dailyTaskService.update(taskId, userId, req);
            }
            return ToolExecutionResult.success("Task '" + response.title() + "' status updated to " + response.status() + ".", response);
        } catch (Exception e) {
            log.error("Failed to update task status: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("Failed to update task status: " + e.getMessage());
        }
    }
}
