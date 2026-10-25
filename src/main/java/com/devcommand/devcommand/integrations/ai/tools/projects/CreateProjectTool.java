package com.devcommand.devcommand.integrations.ai.tools.projects;

import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import com.devcommand.devcommand.projects.dto.ProjectRequest;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreateProjectTool implements AiTool {

    private final ProjectService projectService;

    @Override
    public String getName() {
        return "create_project";
    }

    @Override
    public String getDescription() {
        return "Creates a new software project to track development goals, tasks, and repositories.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Creates a new project",
                ToolParameter.required("name", "string", "Project name"),
                ToolParameter.optional("description", "string", "Project description"),
                ToolParameter.optional("status", "string", "Project status (PLANNING, IN_PROGRESS, COMPLETED, ON_HOLD)"),
                ToolParameter.optional("githubUrl", "string", "GitHub repository URL"),
                ToolParameter.optional("liveUrl", "string", "Live deployment URL")
        );
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        String name = (String) parameters.get("name");
        if (name == null || name.isBlank()) {
            return ToolExecutionResult.failure("Project name is required.");
        }

        String description = parameters.get("description") != null ? parameters.get("description").toString() : null;
        String githubUrl = parameters.get("githubUrl") != null ? parameters.get("githubUrl").toString() : null;
        String liveUrl = parameters.get("liveUrl") != null ? parameters.get("liveUrl").toString() : null;

        ProjectStatus status = ProjectStatus.IN_PROGRESS;
        if (parameters.get("status") != null) {
            try {
                status = ProjectStatus.valueOf(parameters.get("status").toString().toUpperCase());
            } catch (Exception ignored) {}
        }

        ProjectRequest request = new ProjectRequest(
                name,
                description,
                githubUrl,
                liveUrl,
                status,
                LocalDate.now(),
                null
        );

        try {
            ProjectResponse response = projectService.create(request, userId);
            String summary = String.format("Created project '%s' (Status: %s).", response.name(), response.status());
            return ToolExecutionResult.success(summary, response);
        } catch (Exception e) {
            log.error("Failed to create project: {}", e.getMessage(), e);
            return ToolExecutionResult.failure("Failed to create project: " + e.getMessage());
        }
    }
}
