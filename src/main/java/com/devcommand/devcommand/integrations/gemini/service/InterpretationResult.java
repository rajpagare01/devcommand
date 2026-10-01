package com.devcommand.devcommand.integrations.gemini.service;

import com.devcommand.devcommand.command.Command;

public record InterpretationResult(
    InterpretationStatus status,
    Command command,
    String message
) {
    public static InterpretationResult ready(Command command) {
        return new InterpretationResult(InterpretationStatus.READY, command, null);
    }
    
    public static InterpretationResult clarification(String message) {
        return new InterpretationResult(InterpretationStatus.CLARIFICATION_REQUIRED, null, message);
    }
    
    public static InterpretationResult unsupported() {
        return new InterpretationResult(InterpretationStatus.UNSUPPORTED, null, "Not supported");
    }
    
    public static InterpretationResult invalid() {
        return new InterpretationResult(InterpretationStatus.INVALID_MODEL_RESPONSE, null, "I didn't understand that.");
    }
}
