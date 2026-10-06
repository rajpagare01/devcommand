package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.integrations.gemini.config.GeminiProperties;

import com.devcommand.devcommand.integrations.gemini.service.IntentDto;
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

@Service
public class GeminiIntentProvider implements AiIntentProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiIntentProvider.class);

    private final GeminiProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are the DevCommand intent interpreter. Your job is to extract user intent into a structured JSON response.
            Allowed actions:
            - CREATE_TASK: Create a new task explicitly requested by the user. Requires 'title'. Optional: 'description', 'category' (PERSONAL, WORK, LEARNING), 'priority' (LOW, MEDIUM, HIGH, URGENT), 'status' (TODO, IN_PROGRESS, COMPLETED), 'dueDate' (yyyy-MM-dd).
              NOTE: Do NOT use status=COMPLETED just because a user says "I completed a feature". Only set status if the user explicitly asks to create a task with a specific status.
            - READ_PENDING_TASKS: List pending tasks. No parameters needed.
            - COMPLETE_TASK: Mark an EXISTING task as completed. Requires 'title' or 'id'.
              NOTE: Use this ONLY when the user intends to mark an already existing task as done. Do NOT invent a task ID. If there is no identifiable existing task to complete, return a clarificationQuestion.
            - DELETE_TASK: Delete a task. Requires 'title' or 'taskId'.
            - CREATE_DSA_PROBLEM: Track a solved DSA problem. Requires 'platform' (e.g. LEETCODE, HACKERRANK), 'title', 'difficulty' (EASY, MEDIUM, HARD), 'topic', 'status' (SOLVED). Optional: 'timeTaken' (integer).
            - READ_DSA_STATS: Show DSA problem statistics. No parameters needed.
            - CREATE_JOB_APPLICATION: Track a new job application. Requires 'company', 'role', 'status' (SAVED, APPLIED, SCREENING, INTERVIEW, OFFER, REJECTED, WITHDRAWN). Optional: 'location', 'salary', 'source'.
            - READ_JOB_PIPELINE: Show job application pipeline/status counts. No parameters needed.
            - UPDATE_LEARNING_PROGRESS: Update progress on a learning topic. Requires 'topic' (string). Optional: 'progress' (integer 0-100), 'hours' (integer).
              NOTE: Use this ONLY when the user explicitly talks about learning, studying, or reading a topic. Do NOT interpret arbitrary software development or project work as learning progress!
            - READ_LEARNING_PROGRESS: Show learning progress. No parameters needed.
            
            Strict Rules:
            1. PROJECT WORK vs LEARNING: Statements like "I completed the Telegram integration" represent project work, NOT learning progress. If the action is project work but no command perfectly fits (e.g., just logging an accomplishment), set clarificationQuestion instead of forcing it into UPDATE_LEARNING_PROGRESS.
            2. CREATE_TASK vs COMPLETE_TASK: "Create a task to finish X" = CREATE_TASK. "Mark my X task as completed" = COMPLETE_TASK. "I finished X" = COMPLETE_TASK (if X is an existing task) or clarificationQuestion.
            3. NEVER invent unsupported parameters. You must ONLY output parameters explicitly listed as Required or Optional for the chosen action.
            
            If the user request is completely unrelated to DevCommand, set isSupported to false.
            If the user is ambiguous or missing required fields, set clarificationQuestion with a question.
            Do not assume or hallucinate fields.
            """;

    public GeminiIntentProvider(GeminiProperties properties, HttpClient httpClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private static final int MAX_INPUT_LENGTH = 4000;

    @Override
    public String getProviderName() {
        return "gemini";
    }

    @Override
    public ProviderIntentResult interpret(String naturalText, String conversationContext) throws AiProviderException {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiProviderException("Gemini API key is not configured.");
        }

        if (naturalText == null || naturalText.isBlank()) {
            return ProviderIntentResult.invalid();
        }

        if (naturalText.length() > MAX_INPUT_LENGTH) {
            log.warn("Rejected Gemini interpretation request: input length {} exceeds max {}", naturalText.length(), MAX_INPUT_LENGTH);
            return ProviderIntentResult.invalid();
        }

        try {
            String payload = buildRequestPayload(naturalText, conversationContext);
            String url = properties.getApiUrl() + properties.getModel() + ":generateContent?key=" + properties.getApiKey();
            String safeUrl = properties.getApiUrl() + properties.getModel() + ":generateContent?key=***";
            log.info("Requesting Gemini API URL: {}", safeUrl);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            int maxAttempts = properties.getMaxRetries() + 1;
            HttpResponse<String> response = null;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200) {
                        break;
                    } else if (response.statusCode() == 503 || response.statusCode() == 429 || response.statusCode() >= 500) {
                        log.warn("Gemini API transient failure. Status: {} (Attempt {}/{})", response.statusCode(), attempt, maxAttempts);
                        if (attempt == maxAttempts) {
                            throw new TransientAiProviderException("Gemini API returned status " + response.statusCode());
                        }
                        Thread.sleep((long) Math.pow(2, attempt) * 1000);
                    } else {
                        log.error("Gemini API error. Status: {}", response.statusCode());
                        throw new AiProviderException("Gemini API returned status " + response.statusCode());
                    }
                } catch (java.net.http.HttpTimeoutException e) {
                    log.warn("Gemini API timeout. (Attempt {}/{})", attempt, maxAttempts);
                    if (attempt == maxAttempts) {
                        throw new TransientAiProviderException("Gemini API timeout", e);
                    }
                    Thread.sleep((long) Math.pow(2, attempt) * 1000);
                } catch (java.io.IOException e) {
                    log.warn("Gemini API network error: {} (Attempt {}/{})", e.getMessage(), attempt, maxAttempts);
                    if (attempt == maxAttempts) {
                        throw new TransientAiProviderException("Gemini API network error", e);
                    }
                    Thread.sleep((long) Math.pow(2, attempt) * 1000);
                }
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isMissingNode() || candidates.isEmpty()) {
                log.warn("Gemini returned no candidates");
                return ProviderIntentResult.invalid();
            }

            String textContent = candidates.get(0).path("content").path("parts").get(0).path("text").asText();
            IntentDto intent = objectMapper.readValue(textContent, IntentDto.class);

            if (intent.isSupported() != null && !intent.isSupported()) {
                return ProviderIntentResult.unsupported();
            }

            if (intent.clarificationQuestion() != null && !intent.clarificationQuestion().isBlank()) {
                java.util.Map<String, Object> pending = new java.util.HashMap<>();
                if (intent.action() != null) pending.put("action", intent.action());
                if (intent.parameters() != null) pending.put("parameters", intent.parameters());
                return ProviderIntentResult.clarification(intent.clarificationQuestion(), pending);
            }

            if (intent.action() == null) {
                return ProviderIntentResult.invalid();
            }

            return ProviderIntentResult.ready(intent);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TransientAiProviderException("Gemini API request interrupted", e);
        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException("Failed to interpret natural language text", e);
        }
    }

    private String buildRequestPayload(String naturalText, String conversationContext) throws Exception {
        var root = objectMapper.createObjectNode();
        
        var systemInstruction = root.putObject("systemInstruction");
        var sysParts = systemInstruction.putArray("parts");
        
        String prompt = SYSTEM_PROMPT;
        if (conversationContext != null && !conversationContext.isBlank()) {
            prompt += "\n\nCurrent Context:\n" + conversationContext;
            prompt += "\n\nUse the context to fill in missing parameters if the user refers to it implicitly (e.g. 'mark it done', 'update to 50%').";
        }
        
        sysParts.addObject().put("text", prompt);
        
        var contents = root.putArray("contents");
        var content = contents.addObject();
        var parts = content.putArray("parts");
        parts.addObject().put("text", naturalText);
        
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
