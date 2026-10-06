package com.devcommand.devcommand.integrations.ai.service;

public interface AiIntentProvider {
    /**
     * @return provider name (e.g., "GEMINI" or "GROQ")
     */
    String getProviderName();

    ProviderIntentResult interpret(String input, String conversationContext) throws AiProviderException;
}
