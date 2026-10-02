package com.devcommand.devcommand.integrations.gemini.service;

import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.gemini.config.GeminiProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for GeminiIntentInterpreter security and validation behaviour.
 * Tests that do not require a real Gemini API key.
 */
class GeminiIntentInterpreterSecurityTest {

    private GeminiProperties properties;
    private HttpClient httpClient;
    private ObjectMapper objectMapper;
    private GeminiIntentInterpreter interpreter;

    @BeforeEach
    void setUp() {
        properties = mock(GeminiProperties.class);
        httpClient = mock(HttpClient.class);
        objectMapper = new ObjectMapper();
        interpreter = new GeminiIntentInterpreter(properties, httpClient, objectMapper);
    }

    // ------------------------------------------------------------------ API key guards

    @Test
    void interpret_missingApiKey_throwsGeminiApiException() {
        when(properties.getApiKey()).thenReturn(null);
        assertThrows(GeminiApiException.class,
                () -> interpreter.interpret(1L, "add a task"));
    }

    @Test
    void interpret_blankApiKey_throwsGeminiApiException() {
        when(properties.getApiKey()).thenReturn("   ");
        assertThrows(GeminiApiException.class,
                () -> interpreter.interpret(1L, "add a task"));
    }

    // ------------------------------------------------------------------ Input length guards

    @Test
    void interpret_nullInput_returnsInvalidModelResponse() {
        when(properties.getApiKey()).thenReturn("valid-key");
        InterpretationResult result = interpreter.interpret(1L, null);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        // Must never reach the HTTP client
        verifyNoInteractions(httpClient);
    }

    @Test
    void interpret_blankInput_returnsInvalidModelResponse() {
        when(properties.getApiKey()).thenReturn("valid-key");
        InterpretationResult result = interpreter.interpret(1L, "   ");
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        verifyNoInteractions(httpClient);
    }

    @Test
    void interpret_inputExceedsMaxLength_returnsInvalidWithoutCallingApi() {
        when(properties.getApiKey()).thenReturn("valid-key");
        String oversized = "A".repeat(4001);
        InterpretationResult result = interpreter.interpret(1L, oversized);
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
        // No HTTP call must be made
        verifyNoInteractions(httpClient);
    }

    @Test
    void interpret_inputAtExactMaxLength_doesNotRejectBeforeApi() throws Exception {
        // A 4000-char input must reach the HTTP layer (not be short-circuited by the length guard)
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        // Simulate a 500 so we don't need to construct a full valid Gemini response
        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(500);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        String exactMax = "A".repeat(4000);
        assertThrows(GeminiApiException.class,
                () -> interpreter.interpret(1L, exactMax));

        // The HTTP client was actually called - meaning the length guard passed
        verify(httpClient).send(any(), any());
    }

    // ------------------------------------------------------------------ Allowlist enforcement

    @Test
    void interpret_geminiReturnsUnsupportedAction_returnsUnsupported() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        String geminiBody = buildFakeGeminiResponse("{\"isSupported\":true,\"action\":\"CONFIRM_ACTION\",\"parameters\":{}}");

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(geminiBody);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        InterpretationResult result = interpreter.interpret(1L, "confirm my action");
        assertEquals(InterpretationStatus.UNSUPPORTED, result.status());
    }

    @Test
    void interpret_geminiReturnsCancelAction_returnsUnsupported() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        String geminiBody = buildFakeGeminiResponse("{\"isSupported\":true,\"action\":\"CANCEL_ACTION\",\"parameters\":{}}");

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(geminiBody);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        InterpretationResult result = interpreter.interpret(1L, "cancel");
        assertEquals(InterpretationStatus.UNSUPPORTED, result.status());
    }

    @Test
    void interpret_geminiReturnsReadTasksToday_returnsUnsupported() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        String geminiBody = buildFakeGeminiResponse("{\"isSupported\":true,\"action\":\"READ_TASKS_TODAY\",\"parameters\":{}}");

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(geminiBody);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        InterpretationResult result = interpreter.interpret(1L, "show today tasks");
        assertEquals(InterpretationStatus.UNSUPPORTED, result.status());
    }

    @Test
    void interpret_geminiReturnsUnknownAction_returnsInvalidModelResponse() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        String geminiBody = buildFakeGeminiResponse("{\"isSupported\":true,\"action\":\"DROP_TABLE_USERS\",\"parameters\":{}}");

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(geminiBody);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        InterpretationResult result = interpreter.interpret(1L, "drop table");
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
    }

    @Test
    void interpret_geminiReturnsUnsupportedFlag_returnsUnsupported() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        String geminiBody = buildFakeGeminiResponse("{\"isSupported\":false}");

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(geminiBody);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        InterpretationResult result = interpreter.interpret(1L, "tell me a joke");
        assertEquals(InterpretationStatus.UNSUPPORTED, result.status());
    }

    @Test
    void interpret_geminiReturnsClarification_returnsClarification() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        String geminiBody = buildFakeGeminiResponse(
                "{\"isSupported\":true,\"clarificationQuestion\":\"What is the title?\"}");

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(geminiBody);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        InterpretationResult result = interpreter.interpret(1L, "add something");
        assertEquals(InterpretationStatus.CLARIFICATION_REQUIRED, result.status());
        assertEquals("What is the title?", result.message());
    }

    // ------------------------------------------------------------------ userId injection prevention

    @Test
    void interpret_geminiResponseContainsUserId_commandConstructorRejects() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        // Model tries to inject a different userId in parameters
        String geminiBody = buildFakeGeminiResponse(
                "{\"isSupported\":true,\"action\":\"CREATE_TASK\",\"parameters\":{\"title\":\"Test\",\"userId\":999}}");

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(200);
        when(mockResponse.body()).thenReturn(geminiBody);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        // Command constructor calls parameters.rejectKey("userId") -> CommandException ->
        // interpreter wraps it in GeminiApiException
        assertThrows(GeminiApiException.class,
                () -> interpreter.interpret(1L, "create task"));
    }

    // ------------------------------------------------------------------ Gemini API error scenarios

    @Test
    void interpret_gemini429_throwsGeminiApiException() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(429);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        assertThrows(GeminiApiException.class,
                () -> interpreter.interpret(1L, "add a task"));
    }

    @Test
    void interpret_gemini5xx_throwsGeminiApiException() throws Exception {
        when(properties.getApiKey()).thenReturn("valid-key");
        when(properties.getApiUrl()).thenReturn("https://example.com/");
        when(properties.getModel()).thenReturn("gemini");
        when(properties.getTimeoutSeconds()).thenReturn(10);

        @SuppressWarnings("unchecked")
        HttpResponse<String> mockResponse = mock(HttpResponse.class);
        when(mockResponse.statusCode()).thenReturn(503);
        doReturn(mockResponse).when(httpClient).send(any(), any());

        assertThrows(GeminiApiException.class,
                () -> interpreter.interpret(1L, "add a task"));
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

        InterpretationResult result = interpreter.interpret(1L, "something");
        assertEquals(InterpretationStatus.INVALID_MODEL_RESPONSE, result.status());
    }

    // ------------------------------------------------------------------ Helpers

    /**
     * Wraps the given JSON payload into a fake Gemini API response structure.
     */
    private String buildFakeGeminiResponse(String innerJson) throws Exception {
        return "{\"candidates\":[{\"content\":{\"parts\":[{\"text\":" +
                new ObjectMapper().writeValueAsString(innerJson) +
                "}]}}]}";
    }
}
