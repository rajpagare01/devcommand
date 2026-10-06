package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.integrations.gemini.config.GeminiProperties;
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

class GeminiIntentProviderTest {

    private GeminiProperties properties;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private GeminiIntentProvider provider;

    private static final String SUCCESS_RESPONSE = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"action\\":\\"CREATE_TASK\\",\\"parameters\\":{\\"title\\":\\"Test task\\"},\\"isSupported\\":true}"
                      }
                    ]
                  }
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        properties.setApiKey("test-key");
        properties.setMaxRetries(2); // 3 attempts max
        httpClient = mock(HttpClient.class);
        objectMapper = new ObjectMapper();
        provider = new GeminiIntentProvider(properties, httpClient, objectMapper);
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
    void interpret_retriesOn503_then503_thenSuccess() throws Exception {
        HttpResponse<String> mock503 = mock(HttpResponse.class);
        when(mock503.statusCode()).thenReturn(503);
        
        HttpResponse<String> mock200 = mock(HttpResponse.class);
        when(mock200.statusCode()).thenReturn(200);
        when(mock200.body()).thenReturn(SUCCESS_RESPONSE);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock503)
                .thenReturn(mock503)
                .thenReturn(mock200);

        ProviderIntentResult result = provider.interpret("create a task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        verify(httpClient, times(3)).send(any(), any());
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
    void interpret_retriesOn429_thenSuccess() throws Exception {
        HttpResponse<String> mock429 = mock(HttpResponse.class);
        when(mock429.statusCode()).thenReturn(429);
        
        HttpResponse<String> mock200 = mock(HttpResponse.class);
        when(mock200.statusCode()).thenReturn(200);
        when(mock200.body()).thenReturn(SUCCESS_RESPONSE);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock429)
                .thenReturn(mock200);

        ProviderIntentResult result = provider.interpret("create a task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_retriesOnTimeout_thenSuccess() throws Exception {
        HttpResponse<String> mock200 = mock(HttpResponse.class);
        when(mock200.statusCode()).thenReturn(200);
        when(mock200.body()).thenReturn(SUCCESS_RESPONSE);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("timeout"))
                .thenReturn(mock200);

        ProviderIntentResult result = provider.interpret("create a task", null);

        assertEquals(InterpretationStatus.READY, result.status());
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_failsAfterMaxRetriesOnTimeout() throws Exception {
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new HttpTimeoutException("timeout"));

        assertThrows(TransientAiProviderException.class, () -> provider.interpret("create a task", null));
        verify(httpClient, times(3)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_doesNotRetryOn400() throws Exception {
        HttpResponse<String> mock400 = mock(HttpResponse.class);
        when(mock400.statusCode()).thenReturn(400);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock400);

        assertThrows(AiProviderException.class, () -> provider.interpret("create a task", null));
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_doesNotRetryOn401() throws Exception {
        HttpResponse<String> mock401 = mock(HttpResponse.class);
        when(mock401.statusCode()).thenReturn(401);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock401);

        assertThrows(AiProviderException.class, () -> provider.interpret("create a task", null));
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_doesNotRetryOn403() throws Exception {
        HttpResponse<String> mock403 = mock(HttpResponse.class);
        when(mock403.statusCode()).thenReturn(403);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock403);

        assertThrows(AiProviderException.class, () -> provider.interpret("create a task", null));
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_doesNotRetryOn404() throws Exception {
        HttpResponse<String> mock404 = mock(HttpResponse.class);
        when(mock404.statusCode()).thenReturn(404);
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock404);

        assertThrows(AiProviderException.class, () -> provider.interpret("create a task", null));
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_doesNotRetryOnMalformedResponse() throws Exception {
        HttpResponse<String> mock200 = mock(HttpResponse.class);
        when(mock200.statusCode()).thenReturn(200);
        when(mock200.body()).thenReturn("invalid json");
        
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(mock200);

        assertThrows(AiProviderException.class, () -> provider.interpret("create a task", null));
        verify(httpClient, times(1)).send(any(), any());
    }
}
