package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramPendingConfirmation;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramPendingConfirmationRepository;
import com.devcommand.devcommand.tasks.dto.DailyTaskResponse;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class DeleteTaskCommandHandler implements CommandHandler {

    private final DailyTaskService dailyTaskService;
    private final TelegramPendingConfirmationRepository confirmationRepository;
    private final UserRepository userRepository;

    public DeleteTaskCommandHandler(DailyTaskService dailyTaskService,
                                    TelegramPendingConfirmationRepository confirmationRepository,
                                    UserRepository userRepository) {
        this.dailyTaskService = dailyTaskService;
        this.confirmationRepository = confirmationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.DELETE_TASK;
    }

    @Override
    @Transactional
    public CommandResult handle(Command command) {
        Long userId = command.userId();
        Long chatId = null;
        try {
            chatId = Long.parseLong(command.parameters().requiredString("chatId"));
        } catch (Exception e) {
            return CommandResult.failure("Delete task requires a valid chat session.");
        }

        DailyTaskResponse targetTask = null;

        Long taskId = null;
        Optional<String> taskIdOpt = command.parameters().optionalString("taskId");
        if (taskIdOpt.isPresent() && !taskIdOpt.get().isBlank()) {
            try {
                taskId = Long.parseLong(taskIdOpt.get());
            } catch (NumberFormatException ignored) {}
        }

        if (taskId != null) {
            try {
                targetTask = dailyTaskService.getById(taskId, userId);
            } catch (com.devcommand.devcommand.exception.ResourceNotFoundException e) {
                return CommandResult.failure("Task not found or you don't have permission to delete it.");
            }
        } else {
            String title = command.parameters().optionalString("title").orElse(null);
            if (title == null || title.isBlank()) {
                return CommandResult.failure("Please specify a task ID or title to delete.");
            }

            List<DailyTaskResponse> matchingTasks = dailyTaskService.searchPendingTasks(userId, title);
            if (matchingTasks.isEmpty()) {
                return CommandResult.failure("No pending tasks found matching '" + title + "'.");
            } else if (matchingTasks.size() > 1) {
                return CommandResult.failure("Multiple pending tasks match '" + title + "'. Please specify the task ID.");
            }
            targetTask = matchingTasks.get(0);
        }

        // We have a unique task. Create a confirmation token.
        String token = UUID.randomUUID().toString();
        User user = userRepository.findById(userId).orElseThrow();
        
        TelegramPendingConfirmation confirmation = new TelegramPendingConfirmation(
                token,
                user,
                chatId,
                "DELETE_TASK",
                targetTask.id(),
                LocalDateTime.now().plusMinutes(5)
        );
        confirmationRepository.save(confirmation);

        String message = String.format("Are you sure you want to delete task '%s'?\nReply with:\n/confirm %s\n/cancel %s", 
                targetTask.title(), token, token);
                
        return CommandResult.success(message);
    }
}
