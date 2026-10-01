package com.devcommand.devcommand.integrations.gemini.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandParameters;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.gemini.config.GeminiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collections;

@Service
public class GeminiIntentInterpreter implements NaturalLanguageInterpreter {

    private static final Logger log = LoggerFactory.getLogger(GeminiIntentInterpreter.class);

    private final GeminiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are the DevCommand intent interpreter. Your job is to extract user intent into a structured JSON response.
            Allowed actions:
            - CREATE_TASK: Create a new task. Requires 'title'. Optional: 'description', 'category' (PERSONAL, WORK, LEARNING), 'priority' (LOW, MEDIUM, HIGH, URGENT), 'status' (TODO, IN_PROGRESS, DONE, BLOCKED), 'dueDate' (yyyy-MM-dd).
            - READ_PENDING_TASKS: List pending tasks. No parameters needed.
            - COMPLETE_TASK: Mark a task as completed. Requires 'title'.
            - DELETE_TASK: Delete a task. Requires 'title'.
            - CREATE_DSA_PROBLEM: Track a solved DSA problem. Requires 'platform' (e.g. LEETCODE, HACKERRANK), 'title', 'difficulty' (EASY, MEDIUM, HARD), 'topic', 'status' (SOLVED). Optional: 'timeTaken' (integer).
            - READ_DSA_STATS: Show DSA problem statistics. No parameters needed.
            - CREATE_JOB_APPLICATION: Track a new job application. Requires 'company', 'role', 'status' (SAVED, APPLIED, SCREENING, INTERVIEW, OFFER, REJECTED, WITHDRAWN). Optional: 'location', 'salary', 'source'.
            - READ_JOB_PIPELINE: Show job application pipeline/status counts. No parameters needed.
            - UPDATE_LEARNING_PROGRESS: Update progress on a learning topic. Requires 'topic' (string). Optional: 'progress' (integer 0-100), 'hours' (integer).
            - READ_LEARNING_PROGRESS: Show learning progress. No parameters needed.
            
            If the user request is completely unrelated to DevCommand, set isSupported to false.
            If the user is ambiguous or missing required fields, set clarificationQuestion with a question.
            Do not assume or hallucinate fields.
            """;

    public GeminiIntentInterpreter(GeminiProperties properties, HttpClient httpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public InterpretationResult interpret(Long userId, String naturalText) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new GeminiApiException("Gemini API key is not configured.");
        }

        try {
            String payload = buildRequestPayload(naturalText);
            String url = properties.getApiUrl() + properties.getModel() + ":generateContent?key=" + properties.getApiKey();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Gemini API error. Status: {}", response.statusCode());
                throw new GeminiApiException("Gemini API returned status " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isMissingNode() || candidates.isEmpty()) {
                log.warn("Gemini returned no candidates");
                return InterpretationResult.invalid();
            }

            String textContent = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
            IntentDto intent = objectMapper.readValue(textContent, IntentDto.class);

            if (intent.isSupported() != null && !intent.isSupported()) {
                return InterpretationResult.unsupported();
            }

            if (intent.clarificationQuestion() != null && !intent.clarificationQuestion().isBlank()) {
                return InterpretationResult.clarification(intent.clarificationQuestion());
            }

            if (intent.action() == null) {
                return InterpretationResult.invalid();
            }

            CommandType type;
            try {
                type = CommandType.valueOf(intent.action());
            } catch (IllegalArgumentException e) {
                log.warn("Gemini returned unknown action: {}", intent.action());
                return InterpretationResult.invalid();
            }
            
            // Validate allowlist for Phase 1 & 2
            if (type != CommandType.CREATE_TASK && type != CommandType.READ_PENDING_TASKS && type != CommandType.COMPLETE_TASK && type != CommandType.DELETE_TASK && type != CommandType.CREATE_DSA_PROBLEM && type != CommandType.READ_DSA_STATS && type != CommandType.CREATE_JOB_APPLICATION && type != CommandType.READ_JOB_PIPELINE && type != CommandType.UPDATE_LEARNING_PROGRESS && type != CommandType.READ_LEARNING_PROGRESS) {
                log.warn("Gemini returned unsupported action in Phase 1 & 2: {}", type);
                return InterpretationResult.unsupported();
            }

            CommandParameters params = new CommandParameters(intent.parameters() != null ? intent.parameters() : Collections.emptyMap());
            Command command = new Command(type, userId, params);

            return InterpretationResult.ready(command);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new GeminiApiException("Gemini API request interrupted", e);
        } catch (GeminiApiException e) {
            throw e;
        } catch (Exception e) {
            throw new GeminiApiException("Failed to interpret natural language text", e);
        }
    }

    private String buildRequestPayload(String naturalText) throws Exception {
        var root = objectMapper.createObjectNode();
        
        // systemInstruction
        var systemInstruction = root.putObject("systemInstruction");
        var sysParts = systemInstruction.putArray("parts");
        sysParts.addObject().put("text", SYSTEM_PROMPT);
        
        // contents
        var contents = root.putArray("contents");
        var content = contents.addObject();
        var parts = content.putArray("parts");
        parts.addObject().put("text", naturalText);
        
        // generationConfig
        var config = root.putObject("generationConfig");
        config.put("responseMimeType", "application/json");
        
        var schema = config.putObject("responseSchema");
        schema.put("type", "OBJECT");
        
        var props = schema.putObject("properties");
        props.putObject("action").put("type", "STRING");
        props.putObject("parameters").put("type", "OBJECT").put("description", "Extracted parameters");
        props.putObject("clarificationQuestion").put("type", "STRING");
        props.putObject("isSupported").put("type", "BOOLEAN");
        
        var required = schema.putArray("required");
        required.add("isSupported");
        
        return objectMapper.writeValueAsString(root);
    }
}
