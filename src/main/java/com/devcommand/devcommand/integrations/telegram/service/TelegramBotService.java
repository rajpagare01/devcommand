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
                              TelegramBootstrapService bootstrapService) {
        this.properties = properties;
        this.updateProcessor = updateProcessor;
        this.parser = parser;
        this.identityResolver = identityResolver;
        this.processedUpdateRepository = processedUpdateRepository;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.bootstrapService = bootstrapService;
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

        if (properties.getAllowedUserId() != senderId) {
            log.warn("Unauthorized Telegram access attempt from sender ID: {}", senderId);
            sendMessage(chatId, "Unauthorized user.");
            updateProcessor.markProcessed(updateId);
            return true;
        }

        if (text.startsWith("/bootstrap ")) {
            String token = text.substring("/bootstrap ".length()).trim();
            boolean success = bootstrapService.bootstrapAdmin(token, String.valueOf(senderId));
            if (success) {
                sendMessage(chatId, "Bootstrap successful. Your Telegram account is now linked to the admin user. You can now use /start.");
            } else {
                sendMessage(chatId, "Invalid or expired bootstrap token.");
            }
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
            sendMessage(chatId, "Welcome to DevCommand Telegram Bot.\nYou can add tasks by typing: add task: <title>");
            updateProcessor.markProcessed(updateId);
            return true;
        }

        Optional<Command> commandOpt = parser.parse(user.getId(), text);
        if (commandOpt.isEmpty()) {
            sendMessage(chatId, "Unsupported command. Type /help for instructions.");
            updateProcessor.markProcessed(updateId);
            return true;
        }

        try {
            CommandResult result = updateProcessor.processAndMark(commandOpt.get(), updateId);
            if (result.success()) {
                sendMessage(chatId, "Command executed successfully: " + result.message());
            } else {
                sendMessage(chatId, "Command failed: " + result.message());
            }
            return true;
        } catch (Exception e) {
            log.error("Command execution failed", e);
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
            // Escape JSON quotes
            String safeText = text.replace("\"", "\\\"").replace("\n", "\\n");
            String payload = "{\"chat_id\":" + chatId + ",\"text\":\"" + safeText + "\"}";
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
                    
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            log.error("Failed to send Telegram message", e);
        }
    }
}
