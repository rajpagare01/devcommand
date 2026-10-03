package com.devcommand.devcommand.integrations.gemini.service;

import com.devcommand.devcommand.command.Command;
import java.util.Map;

public record InterpretationResult(
    InterpretationStatus status,
    Command command,
    String message,
    Map<String, Object> pendingContext
) {
    public static InterpretationResult ready(Command command) {
        return new InterpretationResult(InterpretationStatus.READY, command, null, null);
    }
    
    public static InterpretationResult clarification(String message, Map<String, Object> pendingContext) {
        return new InterpretationResult(InterpretationStatus.CLARIFICATION_REQUIRED, null, message, pendingContext);
    }
    
    public static InterpretationResult unsupported() {
        return new InterpretationResult(InterpretationStatus.UNSUPPORTED, null, "Not supported", null);
    }
    
    public static InterpretationResult invalid() {
        return new InterpretationResult(InterpretationStatus.INVALID_MODEL_RESPONSE, null, "I didn't understand that.", null);
    }
}
