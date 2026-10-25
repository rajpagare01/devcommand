package com.devcommand.devcommand.integrations.gemini.service;

import com.devcommand.devcommand.command.Command;
import java.util.List;
import java.util.Map;

public record InterpretationResult(
    InterpretationStatus status,
    Command command,
    String message,
    Map<String, Object> pendingContext,
    List<Command> commands
) {
    public InterpretationResult(InterpretationStatus status, Command command, String message, Map<String, Object> pendingContext) {
        this(status, command, message, pendingContext, command != null ? List.of(command) : List.of());
    }

    public static InterpretationResult ready(Command command) {
        return new InterpretationResult(InterpretationStatus.READY, command, null, null, command != null ? List.of(command) : List.of());
    }

    public static InterpretationResult ready(List<Command> commands) {
        Command primary = (commands != null && !commands.isEmpty()) ? commands.get(0) : null;
        return new InterpretationResult(InterpretationStatus.READY, primary, null, null, commands != null ? commands : List.of());
    }
    
    public static InterpretationResult clarification(String message, Map<String, Object> pendingContext) {
        return new InterpretationResult(InterpretationStatus.CLARIFICATION_REQUIRED, null, message, pendingContext, List.of());
    }
    
    public static InterpretationResult unsupported() {
        return new InterpretationResult(InterpretationStatus.UNSUPPORTED, null, "Not supported", null, List.of());
    }
    
    public static InterpretationResult invalid() {
        return new InterpretationResult(InterpretationStatus.INVALID_MODEL_RESPONSE, null, "I didn't understand that.", null, List.of());
    }
}
