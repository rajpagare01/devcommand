package com.devcommand.devcommand.command;

import com.devcommand.devcommand.command.handler.CommandHandler;
import com.devcommand.devcommand.metrics.DevCommandMetrics;
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
    private final DevCommandMetrics metrics;

    public CommandDispatcher(List<CommandHandler> handlers, DevCommandMetrics metrics) {
        this.handlers = handlers;
        this.metrics = metrics;
    }

    public CommandResult dispatch(Command command) {
        if (command == null) {
            throw new CommandException("Command cannot be null");
        }

        String typeName = command.type() != null ? command.type().name() : "UNKNOWN";
        metrics.recordCommandDispatched(typeName);

        try {
            CommandHandler handler = handlers.stream()
                    .filter(h -> h.supports(command.type()))
                    .findFirst()
                    .orElseThrow(() -> new CommandException("Unsupported command: " + command.type()));

            CommandResult result = handler.handle(command);
            if (result.success()) {
                metrics.recordCommandSuccess(typeName);
            } else {
                metrics.recordCommandFailure(typeName);
            }
            return result;
        } catch (CommandException e) {
            metrics.recordCommandFailure(typeName);
            return CommandResult.failure(e.getMessage());
        }
    }
}
