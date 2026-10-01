package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramPendingConfirmationRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CancelActionCommandHandler implements CommandHandler {

    private final TelegramPendingConfirmationRepository confirmationRepository;

    public CancelActionCommandHandler(TelegramPendingConfirmationRepository confirmationRepository) {
        this.confirmationRepository = confirmationRepository;
    }

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.CANCEL_ACTION;
    }

    @Override
    @Transactional
    public CommandResult handle(Command command) {
        Long userId = command.userId();
        Long chatId = null;
        try {
            chatId = Long.parseLong(command.parameters().requiredString("chatId"));
        } catch (Exception e) {
            return CommandResult.failure("Invalid cancellation request.");
        }
        String token = command.parameters().optionalString("token").orElse(null);

        if (token == null || token.isBlank()) {
            return CommandResult.failure("Invalid cancellation request.");
        }

        int deletedRows = confirmationRepository.cancelConfirmation(token, userId, chatId);
        if (deletedRows > 0) {
            return CommandResult.success("Action cancelled.");
        } else {
            return CommandResult.failure("Confirmation token is invalid, belongs to another chat, or has already expired.");
        }
    }
}
