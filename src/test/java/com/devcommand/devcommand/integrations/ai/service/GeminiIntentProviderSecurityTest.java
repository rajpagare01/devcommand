package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.integrations.gemini.config.GeminiProperties;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GeminiIntentProviderSecurityTest {

    private GeminiProperties properties;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private GeminiIntentProvider provider;

    @BeforeEach
    void setUp() {
        properties = mock(GeminiProperties.class);
        httpClient = mock(HttpClient.class);
        objectMapper = new ObjectMapper();
        provider = new GeminiIntentProvider(properties, httpClient, objectMapper);
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
        verifyNoInteractions(httpClient);
    }

    @Test
    void interpret_blankInput_returnsInvalidModelResponse() {
        when(properties.getApiKey()).thenReturn("valid-key");
        ProviderIntentResult result = provider.interpret("   ", null);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        verifyNoInteractions(httpClient);
    }

    @Test
    void interpret_inputExceedsMaxLength_returnsInvalidWithoutCallingApi() {
        when(properties.getApiKey()).thenReturn("valid-key");
        String oversized = "A".repeat(4001);
        ProviderIntentResult result = provider.interpret(oversized, null);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        verifyNoInteractions(httpClient);
    }

    @Test
    void interpret_inputAtExactMaxLength_doesNotRejectBeforeApi() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        String exactMax = "A".repeat(4000);
        assertThrows(AiProviderException.class,
                () -> provider.interpret(exactMax, null));

        verify(httpClient).send(any(), any());
    }

    @Test
    void interpret_geminiEmptyCandidates_returnsInvalidModelResponse() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        String geminiBody = "{\"candidates\":[]}";

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(geminiBody);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        ProviderIntentResult result = provider.interpret("something", null);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
    }
}
