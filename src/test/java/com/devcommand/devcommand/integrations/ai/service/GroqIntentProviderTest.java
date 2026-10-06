package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.integrations.ai.config.GroqProperties;
import com.devcommand.devcommand.integrations.gemini.service.InterpretationStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GroqIntentProviderTest {

    private GroqProperties properties;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private GroqIntentProvider provider;

    private static final String SUCCESS_RESPONSE = """
            {
              "choices": [
                {
                  "message": {
                    "content": "{\\"action\\":\\"CREATE_TASK\\",\\"parameters\\":{\\"title\\":\\"Test task\\"},\\"isSupported\\":true}"
                  }
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        properties = new GroqProperties();
        properties.setApiKey("test-key");
        properties.setMaxRetries(2); // 3 attempts max
        httpClient = mock(HttpClient.class);
        objectMapper = new ObjectMapper();
        provider = new GroqIntentProvider(properties, httpClient, objectMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_returnsReady_whenValidAction_firstAttemptSuccess() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(SUCCESS_RESPONSE);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        ProviderIntentResult result = provider.interpret("create a task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_retriesOn503_thenSuccess() throws Exception {
        HttpResponse<String> mock503 = mock(HttpResponse.class);
        when(mock503.statusCode()).thenReturn(503);
        
        HttpResponse<String> mock200 = mock(HttpResponse.class);
        when(mock200.statusCode()).thenReturn(200);
        when(mock200.body()).thenReturn(SUCCESS_RESPONSE);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock503)
                .thenReturn(mock200);

        ProviderIntentResult result = provider.interpret("create a task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_failsAfterMaxRetriesOn503() throws Exception {
        HttpResponse<String> mock503 = mock(HttpResponse.class);
        when(mock503.statusCode()).thenReturn(503);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock503);

        assertThrows(TransientAiProviderException.class, () -> provider.interpret("create a task", null));
        verify(httpClient, times(3)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_cleansMarkdownJson() throws Exception {
        String markdownResponse = """
            {
              "choices": [
                {
                  "message": {
                    "content": "```json\\n{\\"action\\":\\"CREATE_TASK\\",\\"parameters\\":{\\"title\\":\\"Test task\\"},\\"isSupported\\":true}\\n```"
                  }
                }
              ]
            }
            """;
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(markdownResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        ProviderIntentResult result = provider.interpret("create a task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        assertEquals("CREATE_TASK", result.intent().action());
    }
}
