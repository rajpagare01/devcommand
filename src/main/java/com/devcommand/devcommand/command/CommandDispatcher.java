package com.devcommand.devcommand.command;

import com.devcommand.devcommand.command.handler.CommandHandler;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Dispatches a parsed Command to the appropriate CommandHandler.
 * 
 * Command -> Dispatcher -> Handler -> Existing Service
 * This layer is being introduced for future WhatsApp integration, AI command parsing,
 * and other external integrations.
 */
@Component
public class CommandDispatcher {

    private final List<CommandHandler> handlers;

    public CommandDispatcher(List<CommandHandler> handlers) {
        this.handlers = handlers;
    }

    public CommandResult dispatch(Command command) {
        if (command == null) {
            throw new CommandException("Command cannot be null");
        }

        CommandHandler handler = handlers.stream()
                .filter(h -> h.supports(command.type()))
                .findFirst()
                .orElseThrow(() -> new CommandException("Unsupported command: " + command.type()));

        return handler.handle(command);
    }
}
