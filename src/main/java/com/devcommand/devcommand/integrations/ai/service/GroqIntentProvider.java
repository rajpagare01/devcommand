package com.devcommand.devcommand.integrations.ai.service;

import com.devcommand.devcommand.integrations.ai.config.GroqProperties;
import com.devcommand.devcommand.integrations.gemini.service.IntentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroqIntentProvider implements AiIntentProvider {

    private static final Logger log = LoggerFactory.getLogger(GroqIntentProvider.class);

    private final GroqProperties properties;
    private final ChatModel chatModel;
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
            
            You must respond ONLY with a valid JSON object matching this schema:
            {
              "action": "STRING (or null)",
              "parameters": { ... },
              "clarificationQuestion": "STRING (or null)",
              "isSupported": BOOLEAN
            }
            """;

    public GroqIntentProvider(
            GroqProperties properties,
            @Qualifier("groqChatModel") ChatModel chatModel,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
    }

    private static final int MAX_INPUT_LENGTH = 4000;

    @Override
    public String getProviderName() {
        return "groq";
    }

    @Override
    public ProviderIntentResult interpret(String naturalText, String conversationContext) throws AiProviderException {
        if (!properties.isEnabled()) {
            throw new AiProviderException("Groq provider is disabled.");
        }
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new AiProviderException("Groq API key is not configured.");
        }

        if (naturalText == null || naturalText.isBlank()) {
            return ProviderIntentResult.invalid();
        }

        if (naturalText.length() > MAX_INPUT_LENGTH) {
            log.warn("Rejected Groq interpretation request: input length {} exceeds max {}", naturalText.length(), MAX_INPUT_LENGTH);
            return ProviderIntentResult.invalid();
        }

        try {
            String promptText = SYSTEM_PROMPT;
            if (conversationContext != null && !conversationContext.isBlank()) {
                promptText += "\n\nCurrent Context:\n" + conversationContext;
                promptText += "\n\nUse the context to fill in missing parameters if the user refers to it implicitly (e.g. 'mark it done', 'update to 50%').";
            }

            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(properties.getModel())
                    .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                    .build();

            Prompt prompt = new Prompt(List.of(new SystemMessage(promptText), new UserMessage(naturalText)), options);

            log.info("Requesting Groq intent interpretation for model: {}", properties.getModel());

            ChatResponse response = chatModel.call(prompt);

            if (response == null || response.getResults() == null || response.getResults().isEmpty()
                    || response.getResult() == null || response.getResult().getOutput() == null) {
                log.warn("Groq returned empty response");
                return ProviderIntentResult.invalid();
            }

            String textContent = response.getResult().getOutput().getText();
            if (textContent == null || textContent.isBlank()) {
                log.warn("Groq returned empty text content");
                return ProviderIntentResult.invalid();
            }

            textContent = cleanJson(textContent);

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

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            if (isTransient(e)) {
                throw new TransientAiProviderException("Groq transient failure: " + e.getMessage(), e);
            }
            throw new AiProviderException("Failed to interpret natural language text", e);
        }
    }

    private String cleanJson(String textContent) {
        String trimmed = textContent.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    private boolean isTransient(Throwable t) {
        if (t instanceof TransientAiException) {
            return true;
        }
        if (t instanceof org.springframework.web.client.ResourceAccessException) {
            return true;
        }
        if (t instanceof java.io.IOException || t instanceof java.net.SocketTimeoutException) {
            return true;
        }
        if (t instanceof org.springframework.web.client.HttpStatusCodeException statusEx) {
            return statusEx.getStatusCode().is5xxServerError() || statusEx.getStatusCode().value() == 429;
        }
        Throwable cause = t.getCause();
        if (cause != null && cause != t) {
            return isTransient(cause);
        }
        return false;
    }
}
