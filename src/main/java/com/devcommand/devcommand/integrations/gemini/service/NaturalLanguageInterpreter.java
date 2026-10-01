package com.devcommand.devcommand.integrations.gemini.service;

public interface NaturalLanguageInterpreter {
    InterpretationResult interpret(Long userId, String naturalText);
}
