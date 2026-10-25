package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.integrations.gemini.config.GeminiProperties;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeminiIntentProviderSecurityTest {

    private GeminiProperties properties;
    private ChatModel chatModel;
    private ObjectMapper objectMapper;
    private GeminiIntentProvider provider;

    @BeforeEach
    void setUp() {
        properties = mock(GeminiProperties.class);
        chatModel = mock(ChatModel.class);
        objectMapper = new ObjectMapper();
        provider = new GeminiIntentProvider(properties, chatModel, objectMapper);
    }

    @Test
    void interpret_missingApiKey_throwsAiProviderException() {
        when(properties.getApiKey()).thenReturn(null);
        assertThrows(AiProviderException.class,
                () -> provider.interpret("add a task", null));
    }

    @Test
    void interpret_blankApiKey_throwsAiProviderException() {
        when(properties.getApiKey()).thenReturn("   ");
        assertThrows(AiProviderException.class,
                () -> provider.interpret("add a task", null));
    }

    @Test
    void interpret_nullInput_returnsInvalidModelResponse() {
        when(properties.getApiKey()).thenReturn("valid-key");
        ProviderIntentResult result = provider.interpret(null, null);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        verifyNoInteractions(chatModel);
    }

    @Test
    void interpret_blankInput_returnsInvalidModelResponse() {
        when(properties.getApiKey()).thenReturn("valid-key");
        ProviderIntentResult result = provider.interpret("   ", null);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        verifyNoInteractions(chatModel);
    }

    @Test
    void interpret_inputExceedsMaxLength_returnsInvalidWithoutCallingApi() {
        when(properties.getApiKey()).thenReturn("valid-key");
        String oversized = "A".repeat(4001);
        ProviderIntentResult result = provider.interpret(oversized, null);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        verifyNoInteractions(chatModel);
    }

    @Test
    void interpret_inputAtExactMaxLength_doesNotRejectBeforeApi() {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getModel()).thenReturn("gemini-1.5-flash");
        when(chatModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API error"));

        String exactMax = "A".repeat(4000);
        assertThrows(AiProviderException.class,
                () -> provider.interpret(exactMax, null));

        verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void interpret_geminiEmptyCandidates_returnsInvalidModelResponse() {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getModel()).thenReturn("gemini-1.5-flash");

        ChatResponse emptyResponse = new ChatResponse(Collections.emptyList());
        when(chatModel.call(any(Prompt.class))).thenReturn(emptyResponse);

        ProviderIntentResult result = provider.interpret("something", null);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
    }
}
