package com.devcommand.devcommand.integrations.ai.service;

public class TransientAiProviderException extends AiProviderException {
    public TransientAiProviderException(String message) {
        super(message);
    }
    public TransientAiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
