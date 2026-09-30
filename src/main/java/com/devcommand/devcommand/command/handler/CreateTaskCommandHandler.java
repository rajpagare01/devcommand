package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandException;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.tasks.dto.CreateDailyTaskRequest;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskCategory;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
public class CreateTaskCommandHandler implements CommandHandler {

    private final DailyTaskService dailyTaskService;

    public CreateTaskCommandHandler(DailyTaskService dailyTaskService) {
        this.dailyTaskService = dailyTaskService;
    }

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.CREATE_TASK;
    }

    @Override
    public CommandResult handle(Command command) {
        CommandParameters params = command.parameters();
        
        String title = params.requiredString("title");
        String description = params.optionalString("description").orElse(null);
        TaskCategory category = params.optionalEnum(TaskCategory.class, "category", TaskCategory.PERSONAL);
        TaskPriority priority = params.optionalEnum(TaskPriority.class, "priority", TaskPriority.MEDIUM);
        DailyTaskStatus status = params.optionalEnum(DailyTaskStatus.class, "status", DailyTaskStatus.TODO);
        LocalDate dueDate = params.optionalDate("dueDate").orElse(null);

        CreateDailyTaskRequest request = new CreateDailyTaskRequest(
                title,
                description,
                category,
                priority,
                status,
                dueDate
        );

        DailyTaskResponse response = dailyTaskService.create(request, command.userId());

        return CommandResult.success("Task created successfully", response);
    }
}
