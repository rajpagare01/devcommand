package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.integrations.gemini.service.IntentDto;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationStatus;
import java.util.Map;

public record ProviderIntentResult(
    InterpretationStatus status,
    IntentDto intent,
    String message,
    Map<String, Object> pendingContext
) {
    public static ProviderIntentResult ready(IntentDto intent) {
        return new ProviderIntentResult(InterpretationStatus.READY, intent, null, null);
    }
    public static ProviderIntentResult clarification(String message, Map<String, Object> pendingContext) {
        return new ProviderIntentResult(InterpretationStatus.CLARIFICATION_REQUIRED, null, message, pendingContext);
    }
    public static ProviderIntentResult unsupported() {
        return new ProviderIntentResult(InterpretationStatus.UNSUPPORTED, null, "Not supported", null);
    }
    public static ProviderIntentResult invalid() {
        return new ProviderIntentResult(InterpretationStatus.INVALID_MODEL_RESPONSE, null, "I didn't understand that.", null);
    }
}
