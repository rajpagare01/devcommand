package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class CompleteTaskCommandHandler implements CommandHandler {

    private final DailyTaskService dailyTaskService;

    public CompleteTaskCommandHandler(DailyTaskService dailyTaskService) {
        this.dailyTaskService = dailyTaskService;
    }

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.COMPLETE_TASK;
    }

    @Override
    public CommandResult handle(Command command) {
        CommandParameters params = command.parameters();
        
        Optional<String> idOpt = params.optionalString("id");
        if (idOpt.isPresent()) {
            try {
                Long id = Long.parseLong(idOpt.get());
                DailyTaskResponse response = dailyTaskService.complete(id, command.userId());
                return CommandResult.success("Task '" + response.title() + "' marked as completed.", response);
            } catch (NumberFormatException e) {
                return CommandResult.failure("Invalid task ID format.");
            } catch (com.devcommand.devcommand.exception.ResourceNotFoundException e) {
                return CommandResult.failure("Task not found.");
            }
        }
        
        Optional<String> titleOpt = params.optionalString("title");
        if (titleOpt.isEmpty() || titleOpt.get().isBlank()) {
            return CommandResult.failure("Please specify the task ID or title to complete.");
        }
        
        String title = titleOpt.get();
        List<DailyTaskResponse> matches = dailyTaskService.searchPendingTasks(command.userId(), title);
        
        if (matches.isEmpty()) {
            return CommandResult.failure("No pending tasks found matching '" + title + "'.");
        }
        
        if (matches.size() > 1) {
            return CommandResult.failure("Multiple pending tasks match '" + title + "'. Please be more specific or use the task ID.");
        }
        
        DailyTaskResponse response = dailyTaskService.complete(matches.get(0).id(), command.userId());
        return CommandResult.success("Task '" + response.title() + "' marked as completed.", response);
    }
}
