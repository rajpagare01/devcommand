package com.devcommand.devcommand.command;

public record CommandResult(
        boolean success,
        String message,
        Object data
) {
    public static CommandResult success(String message, Object data) {
        return new CommandResult(true, message, data);
    }
    
    public static CommandResult success(String message) {
        return new CommandResult(true, message, null);
    }

    public static CommandResult failure(String message) {
        return new CommandResult(false, message, null);
    }
}
