package com.devcommand.devcommand.integrations.ai.tools.projects;

import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.dto.UpdateDailyTaskRequest;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
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
        return "Updates the status and/or priority of a task (e.g. COMPLETED, IN_PROGRESS, TODO, HIGH, MEDIUM, LOW).";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Updates status and/or priority of a daily task",
                ToolParameter.optional("id", "integer", "ID of the task"),
                ToolParameter.optional("title", "string", "Title of the task if ID is not known"),
                ToolParameter.optional("status", "string", "New status (COMPLETED, IN_PROGRESS, TODO)"),
                ToolParameter.optional("priority", "string", "New priority (LOW, MEDIUM, HIGH, URGENT)")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String statusStr = parameters.get("status") != null ? parameters.get("status").toString().toUpperCase() : null;
        String priorityStr = parameters.get("priority") != null ? parameters.get("priority").toString().toUpperCase() : null;

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
            // Case 1: Simple completion without priority change
            if ("COMPLETED".equalsIgnoreCase(statusStr) && priorityStr == null) {
                response = dailyTaskService.complete(taskId, userId);
            } else if ("IN_PROGRESS".equalsIgnoreCase(statusStr) && priorityStr == null) {
                response = dailyTaskService.start(taskId, userId);
            } else {
                DailyTaskStatus targetStatus = statusStr != null ? DailyTaskStatus.valueOf(statusStr) : task.getStatus();
                TaskPriority targetPriority = priorityStr != null ? TaskPriority.valueOf(priorityStr) : task.getPriority();

                UpdateDailyTaskRequest req = new UpdateDailyTaskRequest(
                        task.getTitle(),
                        task.getDescription(),
                        task.getCategory(),
                        targetPriority,
                        targetStatus,
                        task.getDueDate()
                );
                response = dailyTaskService.update(taskId, userId, req);
            }

            StringBuilder summary = new StringBuilder("Task '").append(response.title()).append("' updated");
            if (priorityStr != null) {
                summary.append(" (Priority: ").append(response.priority()).append(")");
            }
            if (statusStr != null) {
                summary.append(" (Status: ").append(response.status()).append(")");
            }
            summary.append(".");

            return ToolExecutionResult.success(summary.toString(), response);
        } catch (Exception e) {
            log.error("Failed to update task: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("Failed to update task: " + e.getMessage());
        }
    }
}
