package com.devcommand.devcommand.integrations.gemini.service;

import java.util.Map;

public record IntentDto(
    String action,
    Map<String, Object> parameters,
    String clarificationQuestion,
    Boolean isSupported
) {}
