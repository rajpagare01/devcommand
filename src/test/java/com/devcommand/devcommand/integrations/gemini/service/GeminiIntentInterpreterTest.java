package com.devcommand.devcommand.integrations.gemini.service;

import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.gemini.config.GeminiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GeminiIntentInterpreterTest {

    private GeminiProperties properties;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private GeminiIntentInterpreter interpreter;

    @BeforeEach
    void setUp() {
        properties = new GeminiProperties();
        properties.setApiKey("test-key");
        httpClient = mock(HttpClient.class);
        objectMapper = new ObjectMapper();
        interpreter = new GeminiIntentInterpreter(properties, httpClient, objectMapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_returnsReady_whenValidAction() throws Exception {
        String responseBody = """
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

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        InterpretationResult result = interpreter.interpret(1L, "create a task called Test task");

        assertEquals(InterpretationStatus.READY, result.status());
        assertEquals(CommandType.CREATE_TASK, result.command().type());
        assertEquals("Test task", result.command().parameters().requiredString("title"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_returnsUnsupported_whenIsSupportedFalse() throws Exception {
        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"isSupported\\":false}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        InterpretationResult result = interpreter.interpret(1L, "order me a pizza");

        assertEquals(InterpretationStatus.UNSUPPORTED, result.status());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_returnsClarification_whenClarificationFieldPresent() throws Exception {
        String responseBody = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"clarificationQuestion\\":\\"What should the task title be?\\",\\"isSupported\\":true}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;

        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(responseBody);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        InterpretationResult result = interpreter.interpret(1L, "add a task");

        assertEquals(InterpretationStatus.CLARIFICATION_REQUIRED, result.status());
        assertEquals("What should the task title be?", result.message());
    }

    @Test
    @SuppressWarnings("unchecked")
    void interpret_throwsGeminiApiException_onHttpError() throws Exception {
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(503);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(mockResponse);

        assertThrows(GeminiApiException.class, () -> interpreter.interpret(1L, "hello"));
    }
}
