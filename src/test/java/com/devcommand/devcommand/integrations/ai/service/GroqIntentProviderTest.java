package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.integrations.ai.config.GroqProperties;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroqIntentProviderTest {

    private GroqProperties properties;
    private ChatModel chatModel;
    private ObjectMapper objectMapper;
    private GroqIntentProvider provider;

    private static final String SUCCESS_JSON = """
            {"action":"CREATE_TASK","parameters":{"title":"Test task"},"isSupported":true}
            """;

    @BeforeEach
    void setUp() {
        properties = new GroqProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setModel("llama-3.3-70b-versatile");
        chatModel = mock(ChatModel.class);
        objectMapper = new ObjectMapper();
        provider = new GroqIntentProvider(properties, chatModel, objectMapper);
    }

    private ChatResponse createChatResponse(String content) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    @Test
    void interpret_returnsReady_whenValidAction() {
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse(SUCCESS_JSON));

        ProviderIntentResult result = provider.interpret("create a task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        assertNotNull(result.intent());
        assertEquals("CREATE_TASK", result.intent().action());
        assertEquals("Test task", result.intent().parameters().get("title"));
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void interpret_throwsTransientAiProviderException_onTransientAiException() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new TransientAiException("rate limit or 503"));

        assertThrows(TransientAiProviderException.class, () -> provider.interpret("create a task", null));
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void interpret_throwsTransientAiProviderException_onTimeout() {
        when(chatModel.call(any(Prompt.class)))
                .thenThrow(new ResourceAccessException("Read timed out", new SocketTimeoutException("timeout")));

        assertThrows(TransientAiProviderException.class, () -> provider.interpret("create a task", null));
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void interpret_throwsAiProviderException_onNonTransientAiException() {
        when(chatModel.call(any(Prompt.class))).thenThrow(new NonTransientAiException("bad request 400"));

        assertThrows(AiProviderException.class, () -> provider.interpret("create a task", null));
        verify(chatModel, times(1)).call(any(Prompt.class));
    }

    @Test
    void interpret_throwsAiProviderException_whenDisabled() {
        properties.setEnabled(false);

        assertThrows(AiProviderException.class, () -> provider.interpret("create a task", null));
        verifyNoInteractions(chatModel);
    }

    @Test
    void interpret_throwsAiProviderException_whenMissingApiKey() {
        properties.setApiKey(null);

        assertThrows(AiProviderException.class, () -> provider.interpret("create a task", null));
        verifyNoInteractions(chatModel);
    }

    @Test
    void interpret_cleansMarkdownJson() {
        String markdownJson = "```json\n" + SUCCESS_JSON + "\n```";
        when(chatModel.call(any(Prompt.class))).thenReturn(createChatResponse(markdownJson));

        ProviderIntentResult result = provider.interpret("create a task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        assertEquals("CREATE_TASK", result.intent().action());
    }
}
