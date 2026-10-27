package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.dto.UpdateDailyTaskRequest;
import com.devcommand.devcommand.tasks.entity.DailyTask;
import com.devcommand.devcommand.tasks.entity.DailyTaskStatus;
import com.devcommand.devcommand.tasks.entity.TaskPriority;
import com.devcommand.devcommand.tasks.repository.DailyTaskRepository;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UpdateTaskCommandHandler implements CommandHandler {

    private final DailyTaskService dailyTaskService;
    private final DailyTaskRepository dailyTaskRepository;

    public UpdateTaskCommandHandler(DailyTaskService dailyTaskService, DailyTaskRepository dailyTaskRepository) {
        this.dailyTaskService = dailyTaskService;
        this.dailyTaskRepository = dailyTaskRepository;
    }

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.UPDATE_TASK;
    }

    @Override
    public CommandResult handle(Command command) {
        CommandParameters params = command.parameters();
        Long userId = command.userId();

        Optional<DailyTask> taskOpt = Optional.empty();
        Optional<String> idOpt = params.optionalString("id");
        if (idOpt.isPresent()) {
            try {
                Long id = Long.parseLong(idOpt.get());
                taskOpt = dailyTaskRepository.findByIdAndUserId(id, userId);
            } catch (NumberFormatException ignored) {}
        }

        if (taskOpt.isEmpty()) {
            Optional<String> titleOpt = params.optionalString("title");
            if (titleOpt.isPresent() && !titleOpt.get().isBlank()) {
                taskOpt = dailyTaskRepository.findFirstByUserIdAndTitleIgnoreCase(userId, titleOpt.get());
            }
        }

        if (taskOpt.isEmpty()) {
            return CommandResult.failure("Task not found.");
        }

        DailyTask task = taskOpt.get();
        Long taskId = task.getId();

        TaskPriority priority = task.getPriority();
        Optional<String> priorityOpt = params.optionalString("priority");
        if (priorityOpt.isPresent()) {
            try {
                priority = TaskPriority.valueOf(priorityOpt.get().toUpperCase());
            } catch (Exception ignored) {}
        }

        DailyTaskStatus status = task.getStatus();
        Optional<String> statusOpt = params.optionalString("status");
        if (statusOpt.isPresent()) {
            try {
                status = DailyTaskStatus.valueOf(statusOpt.get().toUpperCase());
            } catch (Exception ignored) {}
        }

        UpdateDailyTaskRequest req = new UpdateDailyTaskRequest(
                task.getTitle(),
                task.getDescription(),
                task.getCategory(),
                priority,
                status,
                task.getDueDate()
        );

        try {
            DailyTaskResponse response = dailyTaskService.update(taskId, userId, req);
            return CommandResult.success("Task '" + response.title() + "' updated successfully.", response);
        } catch (Exception e) {
            return CommandResult.failure("Failed to update task: " + e.getMessage());
        }
    }
}
