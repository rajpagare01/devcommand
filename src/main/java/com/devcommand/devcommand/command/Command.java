package com.devcommand.devcommand.command;

import java.util.Collections;
import java.util.Map;

/**
 * An immutable DTO representing a parsed command.
 * The userId must ALWAYS be supplied from the authenticated security context,
 * not from the parsed external input.
 */
public record Command(
        CommandType type,
        Long userId,
        CommandParameters parameters
) {
    public Command {
        if (type == null) {
            throw new IllegalArgumentException("Command type cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        if (parameters == null) {
            parameters = new CommandParameters(Collections.emptyMap());
        }
        
        // Security check: ensure the payload didn't attempt to sneak in a userId
        parameters.rejectKey("userId");
    }
    
    // Convenience constructor for tests
    public Command(CommandType type, Long userId, Map<String, ?> rawParams) {
        this(type, userId, new CommandParameters(rawParams));
    }
}
