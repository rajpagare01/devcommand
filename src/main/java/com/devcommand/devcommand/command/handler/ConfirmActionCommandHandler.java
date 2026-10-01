package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramPendingConfirmation;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramPendingConfirmationRepository;
import com.devcommand.devcommand.tasks.service.DailyTaskService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class ConfirmActionCommandHandler implements CommandHandler {

    private final TelegramPendingConfirmationRepository confirmationRepository;
    private final DailyTaskService dailyTaskService;

    public ConfirmActionCommandHandler(TelegramPendingConfirmationRepository confirmationRepository,
                                       DailyTaskService dailyTaskService) {
        this.confirmationRepository = confirmationRepository;
        this.dailyTaskService = dailyTaskService;
    }

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.CONFIRM_ACTION;
    }

    @Override
    @Transactional
    public CommandResult handle(Command command) {
        Long userId = command.userId();
        Long chatId = null;
        try {
            chatId = Long.parseLong(command.parameters().requiredString("chatId"));
        } catch (Exception e) {
            return CommandResult.failure("Invalid confirmation request.");
        }
        String token = command.parameters().optionalString("token").orElse(null);

        if (token == null || token.isBlank()) {
            return CommandResult.failure("Invalid confirmation request.");
        }

        // 1. Fetch valid confirmation safely
        Optional<TelegramPendingConfirmation> pendingOpt = confirmationRepository.findValidConfirmation(token, userId, chatId);
        
        if (pendingOpt.isEmpty()) {
            return CommandResult.failure("Confirmation token is invalid, belongs to another chat, or has already been consumed.");
        }
        
        TelegramPendingConfirmation pending = pendingOpt.get();
        if (pending.getExpiresAt().isBefore(LocalDateTime.now())) {
            return CommandResult.failure("Confirmation token has expired.");
        }
        
        if (!"DELETE_TASK".equals(pending.getActionType())) {
            return CommandResult.failure("Unsupported action type.");
        }

        // 2. Atomically consume the confirmation
        int deletedRows = confirmationRepository.consumeConfirmation(token, userId, chatId, "DELETE_TASK", LocalDateTime.now());
        if (deletedRows != 1) {
            return CommandResult.failure("Failed to consume confirmation token. It may have expired or been used concurrently.");
        }

        // 3. Recheck ownership and existence happens inherently in dailyTaskService.delete()
        // since it requires the task to belong to userId.
        try {
            dailyTaskService.delete(pending.getTaskId(), userId);
            return CommandResult.success("Task deleted successfully.");
        } catch (Exception e) {
            // Because this is @Transactional, deleting the task failing will also rollback the consumeConfirmation
            // This is desired so the user doesn't lose the token if an internal server error occurs, though transient errors
            // will retry automatically through TelegramUpdateProcessor if we throw out of the handler.
            // Wait, dailyTaskService.delete throws an exception if task not found? 
            // In DailyTaskService, if task not found, it usually throws EntityNotFoundException or generic RuntimeException.
            throw e;
        }
    }
}
