package com.devcommand.devcommand.integrations.telegram.service;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandDispatcher;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.identity.ExternalIdentity;
import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.command.identity.ExternalIdentityResolver;
import com.devcommand.devcommand.integrations.telegram.config.TelegramProperties;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramProcessedUpdate;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramProcessedUpdateRepository;
import com.devcommand.devcommand.integrations.whatsapp.service.DeterministicCommandParser;
import com.devcommand.devcommand.integrations.gemini.service.NaturalLanguageInterpreter;
import com.devcommand.devcommand.conversation.service.ConversationContextService;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.user.entity.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(name = "telegram.bot-token")
public class TelegramBotService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);
    private static final String API_URL = "https://api.telegram.org/bot";

    private final TelegramProperties properties;
    private final TelegramUpdateProcessor updateProcessor;
    private final DeterministicCommandParser parser;
    private final ExternalIdentityResolver identityResolver;
    private final TelegramProcessedUpdateRepository processedUpdateRepository;
    private final ObjectMapper objectMapper;
    private final TelegramBootstrapService bootstrapService;
    private final NaturalLanguageInterpreter interpreter;
    private final ConversationContextService contextService;

    private final HttpClient httpClient;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executor;
    private long lastUpdateId = 0;

    public TelegramBotService(TelegramProperties properties,
                              TelegramUpdateProcessor updateProcessor,
                              DeterministicCommandParser parser,
                              ExternalIdentityResolver identityResolver,
                              TelegramProcessedUpdateRepository processedUpdateRepository,
                              ObjectMapper objectMapper,
                              HttpClient httpClient,
                              TelegramBootstrapService bootstrapService,
                              NaturalLanguageInterpreter interpreter,
                              ConversationContextService contextService) {
        this.properties = properties;
        this.updateProcessor = updateProcessor;
        this.parser = parser;
        this.identityResolver = identityResolver;
        this.processedUpdateRepository = processedUpdateRepository;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.bootstrapService = bootstrapService;
        this.interpreter = interpreter;
        this.contextService = contextService;
    }

    @PostConstruct
    public void start() {
        if (properties.getBotToken() == null || properties.getBotToken().isBlank() || properties.getAllowedUserId() == null) {
            log.warn("Telegram integration enabled but token or allowed user ID is missing.");
            return;
        }

        running.set(true);
        executor = Executors.newSingleThreadExecutor();
        executor.submit(this::pollUpdates);
        log.info("Telegram Bot polling started.");
    }

    @PreDestroy
    public void stop() {
        running.set(false);
        if (executor != null) {
            executor.shutdownNow();
        }
        log.info("Telegram Bot polling stopped.");
    }

    private void pollUpdates() {
        while (running.get()) {
            try {
                String url = API_URL + properties.getBotToken() + "/getUpdates?timeout=30&offset=" + (lastUpdateId + 1);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(40))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 200) {
                    processUpdates(response.body());
                } else {
                    log.error("Telegram API error: HTTP {}", response.statusCode());
                    Thread.sleep(5000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Telegram polling interrupted.");
                break;
            } catch (Exception e) {
                log.error("Error polling Telegram updates", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processUpdates(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            if (root.path("ok").asBoolean()) {
                JsonNode results = root.path("result");
                for (JsonNode update : results) {
                    long updateId = update.path("update_id").asLong();

                    if (processedUpdateRepository.existsById(updateId)) {
                        this.lastUpdateId = Math.max(this.lastUpdateId, updateId);
                        continue;
                    }

                    JsonNode message = update.path("message");
                    if (!message.isMissingNode()) {
                        boolean success = handleMessage(updateId, message);
                        if (!success) {
                            // Systemic failure (e.g. database down), break out of the loop
                            // so we can retry this update later.
                            break;
                        }
                    } else {
                        // Mark non-message updates as processed
                        updateProcessor.markProcessed(updateId);
                    }
                    this.lastUpdateId = Math.max(this.lastUpdateId, updateId);
                }
            }
        } catch (Exception e) {
            log.error("Failed to process Telegram JSON updates", e);
        }
    }

    private boolean handleMessage(long updateId, JsonNode message) {
        JsonNode from = message.path("from");
        JsonNode chat = message.path("chat");
        JsonNode textNode = message.path("text");

        if (from.isMissingNode() || chat.isMissingNode() || textNode.isMissingNode()) {
            updateProcessor.markProcessed(updateId);
            return true;
        }
        
        String type = chat.path("type").asText();
        if (!"private".equals(type)) {
            log.warn("Ignoring message from non-private chat");
            updateProcessor.markProcessed(updateId);
            return true;
        }

        long senderId = from.path("id").asLong();
        long chatId = chat.path("id").asLong();
        String text = textNode.asText();

        if (text.startsWith("/bootstrap ")) {
            String token = text.substring("/bootstrap ".length()).trim();
            String bootstrapResult = bootstrapService.linkAccount(token, String.valueOf(senderId));
            sendMessage(chatId, bootstrapResult);
            updateProcessor.markProcessed(updateId);
            return true;
        }

        if (properties.getAllowedUserId() != null && properties.getAllowedUserId() != senderId) {
            log.warn("Unauthorized Telegram access attempt from sender ID: {}", senderId);
            sendMessage(chatId, "Unauthorized user.");
            updateProcessor.markProcessed(updateId);
            return true;
        }

        ExternalIdentity identity = new ExternalIdentity(ExternalIdentityProvider.TELEGRAM, String.valueOf(senderId));
        Optional<User> userOpt;
        try {
            userOpt = identityResolver.resolve(identity);
        } catch (Exception e) {
            log.error("Database failure while resolving identity", e);
            return false; // Return false to break the batch and retry
        }

        if (userOpt.isEmpty()) {
            log.warn("Verified DevCommand user not found for Telegram sender ID: {}", senderId);
            sendMessage(chatId, "User identity not mapped or verified. Please configure your Telegram identity.");
            updateProcessor.markProcessed(updateId);
            return true;
        }

        User user = userOpt.get();

        if (text.startsWith("/start") || text.startsWith("/help")) {
            sendMessage(chatId, "Welcome to DevCommand Telegram Bot.\nYou can add tasks by typing: add task: <title>\nOr just type your request naturally!");
            updateProcessor.markProcessed(updateId);
            return true;
        }

        Command finalCommand = null;
        if (text.startsWith("/confirm ")) {
            String token = text.substring("/confirm ".length()).trim();
            finalCommand = new Command(com.devcommand.devcommand.command.CommandType.CONFIRM_ACTION, user.getId(), new com.devcommand.devcommand.command.CommandParameters(java.util.Map.of("token", token, "chatId", chatId)));
        } else if (text.startsWith("/cancel ")) {
            String token = text.substring("/cancel ".length()).trim();
            finalCommand = new Command(com.devcommand.devcommand.command.CommandType.CANCEL_ACTION, user.getId(), new com.devcommand.devcommand.command.CommandParameters(java.util.Map.of("token", token, "chatId", chatId)));
        } else {
            Optional<Command> commandOpt = parser.parse(user.getId(), text);
            
            if (commandOpt.isPresent()) {
                finalCommand = commandOpt.get();
            } else {
                // Fallback to Gemini
                try {
                    String contextJson = null;
                    Optional<com.devcommand.devcommand.conversation.entity.ConversationContext> contextOpt = contextService.getContext(user.getId(), String.valueOf(chatId));
                    if (contextOpt.isPresent()) {
                        try {
                            contextJson = objectMapper.writeValueAsString(contextOpt.get().getContextData());
                        } catch (Exception ignored) {}
                    }
                    
                    com.devcommand.devcommand.integrations.gemini.service.InterpretationResult result = interpreter.interpret(user.getId(), text, contextJson);
                    switch (result.status()) {
                        case READY:
                            finalCommand = result.command();
                            break;
                        case CLARIFICATION_REQUIRED:
                            if (result.pendingContext() != null && !result.pendingContext().isEmpty()) {
                                contextService.updateContext(user.getId(), String.valueOf(chatId), ContextType.PENDING_CLARIFICATION, result.pendingContext());
                            }
                            sendMessage(chatId, result.message());
                            updateProcessor.markProcessed(updateId);
                            return true;
                        case UNSUPPORTED:
                            sendMessage(chatId, "Sorry, I can't help with that yet. Try asking about tasks!");
                            updateProcessor.markProcessed(updateId);
                            return true;
                        case INVALID_MODEL_RESPONSE:
                            sendMessage(chatId, "I'm having trouble understanding that. Could you rephrase?");
                            updateProcessor.markProcessed(updateId);
                            return true;
                    }
                } catch (com.devcommand.devcommand.integrations.gemini.service.GeminiApiException e) {
                    log.error("Gemini API infrastructure failure", e);
                    sendMessage(chatId, "My AI assistant is temporarily unavailable. Please use the exact command format (e.g., 'add task: title').");
                    // Do not mark as processed if we want to retry? Actually, this is a user-facing action, we probably want to mark it 
                    // so they can try again, rather than repeatedly erroring on the same message and blocking the queue.
                    updateProcessor.markProcessed(updateId);
                    return true;
                } catch (Exception e) {
                    log.error("Unexpected error during NLP interpretation", e);
                    sendMessage(chatId, "An error occurred while processing your request naturally.");
                    updateProcessor.markProcessed(updateId);
                    return true;
                }
            }
        }

        if (finalCommand == null) {
            sendMessage(chatId, "Unsupported command. Type /help for instructions.");
            updateProcessor.markProcessed(updateId);
            return true;
        }

        // Inject chatId into every command parameters
        java.util.Map<String, Object> newParams = new java.util.HashMap<>(finalCommand.parameters().asMap());
        newParams.put("chatId", chatId);
        finalCommand = new Command(finalCommand.type(), finalCommand.userId(), new com.devcommand.devcommand.command.CommandParameters(newParams));


        try {
            CommandResult result = updateProcessor.processAndMark(finalCommand, updateId);
            if (result.success()) {
                sendMessage(chatId, "✅ " + result.message());
                updateContextAfterSuccess(user.getId(), String.valueOf(chatId), finalCommand.type(), result.data());
            } else {
                sendMessage(chatId, "❌ " + result.message());
            }
            return true;
        } catch (org.springframework.dao.TransientDataAccessException | org.springframework.transaction.TransactionException transientE) {
            log.error("Transient error during command execution, will retry", transientE);
            // Do not mark as processed so it can be retried on next poll.
            return false;
        } catch (Exception e) {
            log.error("Command execution failed permanently", e);
            sendMessage(chatId, "Command execution failed due to an internal error.");
            try {
                updateProcessor.markProcessed(updateId);
                return true;
            } catch (Exception innerE) {
                log.error("Failed to mark update as processed after command failure", innerE);
                return false; // Transient failure to mark processed, break batch
            }
        }
    }

    private void sendMessage(long chatId, String text) {
        try {
            String url = API_URL + properties.getBotToken() + "/sendMessage";
            
            java.util.Map<String, Object> payloadMap = new java.util.HashMap<>();
            payloadMap.put("chat_id", chatId);
            payloadMap.put("text", text);
            String payload = objectMapper.writeValueAsString(payloadMap);
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
                    
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String errorDescription = "Unknown error";
                try {
                    JsonNode root = objectMapper.readTree(response.body());
                    if (root.has("description")) {
                        errorDescription = root.get("description").asText();
                    }
                } catch (Exception ignored) {}
                log.error("Failed to send Telegram message. HTTP {}: {}", response.statusCode(), errorDescription);
            }
        } catch (Exception e) {
            log.error("Exception while sending Telegram message", e);
        }
    }

    private void updateContextAfterSuccess(Long userId, String chatId, com.devcommand.devcommand.command.CommandType type, Object responseData) {
        if (responseData == null) return;
        try {
            java.util.Map<String, Object> dataMap = objectMapper.convertValue(responseData, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {});
            switch (type) {
                case CREATE_TASK -> contextService.updateContext(userId, chatId, ContextType.LAST_TASK, dataMap);
                case CREATE_DSA_PROBLEM -> contextService.updateContext(userId, chatId, ContextType.LAST_DSA_PROBLEM, dataMap);
                case CREATE_JOB_APPLICATION -> contextService.updateContext(userId, chatId, ContextType.LAST_JOB_APPLICATION, dataMap);
                case UPDATE_LEARNING_PROGRESS -> contextService.updateContext(userId, chatId, ContextType.LAST_LEARNING_TOPIC, dataMap);
                default -> {}
            }
        } catch (Exception e) {
            log.warn("Failed to update context after success", e);
        }
    }
}
