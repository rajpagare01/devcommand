package com.devcommand.devcommand.integrations.gemini.service;

import java.util.List;
import java.util.Map;

public record IntentDto(
    String action,
    Map<String, Object> parameters,
    String clarificationQuestion,
    Boolean isSupported,
    List<IntentDto> actions
) {
    public IntentDto(String action, Map<String, Object> parameters, String clarificationQuestion, Boolean isSupported) {
        this(action, parameters, clarificationQuestion, isSupported, null);
    }
}
