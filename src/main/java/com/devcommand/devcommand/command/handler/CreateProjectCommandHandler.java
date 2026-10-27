package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.projects.dto.ProjectRequest;
import com.devcommand.devcommand.projects.dto.ProjectResponse;
import com.devcommand.devcommand.projects.entity.ProjectStatus;
import com.devcommand.devcommand.projects.service.ProjectService;
import org.springframework.stereotype.Component;

@Component
public class CreateProjectCommandHandler implements CommandHandler {

    private final ProjectService projectService;

    public CreateProjectCommandHandler(ProjectService projectService) {
        this.projectService = projectService;
    }

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.CREATE_PROJECT;
    }

    @Override
    public CommandResult handle(Command command) {
        CommandParameters params = command.parameters();
        String name = params.optionalString("name").orElse("Untitled Project");
        String description = params.optionalString("description").orElse(null);
        String githubUrl = params.optionalString("githubUrl").orElse(null);
        String liveUrl = params.optionalString("liveUrl").orElse(null);

        ProjectStatus status = ProjectStatus.IN_PROGRESS;
        if (params.optionalString("status").isPresent()) {
            try {
                status = ProjectStatus.valueOf(params.optionalString("status").get().toUpperCase());
            } catch (Exception ignored) {}
        }

        ProjectRequest req = new ProjectRequest(name, description, githubUrl, liveUrl, status, null, null);
        try {
            ProjectResponse response = projectService.create(req, command.userId());
            return CommandResult.success("Project '" + response.name() + "' created successfully.", response);
        } catch (Exception e) {
            return CommandResult.failure("Failed to create project: " + e.getMessage());
        }
    }
}
