package com.devcommand.devcommand.integrations.ai.tools.projects;

import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.projects.dto.ProjectTaskRequest;
import com.devcommand.devcommand.projects.dto.ProjectTaskResponse;
import com.devcommand.devcommand.projects.entity.ProjectTaskPriority;
import com.devcommand.devcommand.projects.entity.ProjectTaskStatus;
import com.devcommand.devcommand.projects.service.ProjectTaskService;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateTaskTool implements AiTool {

    private final DailyTaskService dailyTaskService;
    private final ProjectTaskService projectTaskService;

    @Override
    public String getName() {
        return "create_task";
    }

    @Override
    public String getDescription() {
        return "Creates a new daily task or project task with title, category, priority, status, and due date.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Creates a new task",
                ToolParameter.required("title", "string", "Task title"),
                ToolParameter.optional("description", "string", "Task description"),
                ToolParameter.optional("category", "string", "Category (PERSONAL, WORK, LEARNING)"),
                ToolParameter.optional("priority", "string", "Priority (LOW, MEDIUM, HIGH, URGENT)"),
                ToolParameter.optional("status", "string", "Status (TODO, IN_PROGRESS, COMPLETED)"),
                ToolParameter.optional("dueDate", "date", "Due date (YYYY-MM-DD)"),
                ToolParameter.optional("projectId", "integer", "Optional parent project ID")
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
            return ToolExecutionResult.failure("Task title is required.");
        }

        String description = parameters.get("description") != null ? parameters.get("description").toString() : null;

        LocalDate dueDate = null;
        if (parameters.get("dueDate") != null) {
            try {
                dueDate = LocalDate.parse(parameters.get("dueDate").toString().trim());
            } catch (Exception ignored) {}
        }

        Long projectId = null;
        if (parameters.get("projectId") != null) {
            try {
                projectId = Long.parseLong(parameters.get("projectId").toString());
            } catch (Exception ignored) {}
        }

        if (projectId != null) {
            ProjectTaskStatus status = ProjectTaskStatus.TODO;
            if (parameters.get("status") != null) {
                try {
                    status = ProjectTaskStatus.valueOf(parameters.get("status").toString().toUpperCase());
                } catch (Exception ignored) {}
            }
            ProjectTaskPriority priority = ProjectTaskPriority.MEDIUM;
            if (parameters.get("priority") != null) {
                try {
                    priority = ProjectTaskPriority.valueOf(parameters.get("priority").toString().toUpperCase());
                } catch (Exception ignored) {}
            }

            ProjectTaskRequest req = new ProjectTaskRequest(title, description, status, priority, dueDate);
            try {
                ProjectTaskResponse resp = projectTaskService.create(projectId, req, userId);
                return ToolExecutionResult.success("Created project task '" + resp.title() + "' in project #" + projectId + ".", resp);
            } catch (Exception e) {
                log.error("Failed to create project task: {}", e.getMessage(), e);
                return ToolExecutionResult.failure("Failed to create project task: " + e.getMessage());
            }
        } else {
            TaskCategory category = TaskCategory.PERSONAL;
            if (parameters.get("category") != null) {
                try {
                    category = TaskCategory.valueOf(parameters.get("category").toString().toUpperCase());
                } catch (Exception ignored) {}
            }

            TaskPriority priority = TaskPriority.MEDIUM;
            if (parameters.get("priority") != null) {
                try {
                    priority = TaskPriority.valueOf(parameters.get("priority").toString().toUpperCase());
                } catch (Exception ignored) {}
            }

            DailyTaskStatus status = DailyTaskStatus.TODO;
            if (parameters.get("status") != null) {
                try {
                    status = DailyTaskStatus.valueOf(parameters.get("status").toString().toUpperCase());
                } catch (Exception ignored) {}
            }

            CreateDailyTaskRequest req = new CreateDailyTaskRequest(title, description, category, priority, status, dueDate);
            try {
                DailyTaskResponse resp = dailyTaskService.create(req, userId);
                return ToolExecutionResult.success("Created task '" + resp.title() + "' (" + resp.priority() + " priority).", resp);
            } catch (Exception e) {
                log.error("Failed to create daily task: {}", e.getMessage(), e);
                return ToolExecutionResult.failure("Failed to create daily task: " + e.getMessage());
            }
        }
    }
}
