package com.devcommand.devcommand.conversation.service;

import com.devcommand.devcommand.conversation.entity.ConversationContext;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.model.ContextualEntity;
import com.devcommand.devcommand.conversation.model.ConversationMemory;
import com.devcommand.devcommand.conversation.model.ConversationMessage;
import com.devcommand.devcommand.conversation.repository.ConversationContextRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
public class ConversationContextService {

    private static final String KEY_ENTITIES = "entities";
    private static final String KEY_MESSAGES = "messages";

    private final ConversationContextRepository repository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Value("${devcommand.conversation.ttl-minutes:30}")
    private long ttlMinutes;

    @Value("${ai.conversation.max-history-messages:20}")
    private int maxHistoryMessages = 20;

    public ConversationContextService(ConversationContextRepository repository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        try {
            this.objectMapper.findAndRegisterModules();
        } catch (Exception ignored) {}
    }

    @Transactional(readOnly = true)
    public boolean isOwnedByAnotherUser(String chatId, Long userId) {
        if (chatId == null || chatId.isBlank() || userId == null) {
            return false;
        }
        boolean exists = repository.existsByChatIdAndUserIdNot(chatId.trim(), userId);
        if (exists) {
            log.warn("Security alert: userId {} attempted to access conversationId '{}' owned by another user", userId, chatId);
        }
        return exists;
    }

    @Transactional
    public Optional<ConversationContext> getContext(Long userId, String chatId) {
        if (userId == null || chatId == null || chatId.isBlank()) {
            return Optional.empty();
        }
        log.debug("Context lookup for user {} conversation {}", userId, chatId);
        Optional<ConversationContext> optContext = repository.findByUserIdAndChatId(userId, chatId.trim());
        if (optContext.isEmpty()) {
            log.debug("Context miss for user {} conversation {}", userId, chatId);
            return Optional.empty();
        }

        ConversationContext context = optContext.get();
        if (context.getExpiresAt().isBefore(ZonedDateTime.now())) {
            log.info("Conversation context expired for user {} conversation {}, purging", userId, chatId);
            repository.delete(context);
            return Optional.empty();
        }

        log.debug("Context hit for user {} conversation {}", userId, chatId);
        return Optional.of(context);
    }

    @Transactional
    public Optional<ConversationContext> getLatestContext(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        return repository.findTopByUserIdAndExpiresAtAfterOrderByUpdatedAtDesc(userId, ZonedDateTime.now());
    }

    @Transactional
    public Optional<ConversationMemory> getMemory(Long userId, String chatId) {
        Optional<ConversationContext> optContext = getContext(userId, chatId);
        if (optContext.isEmpty()) {
            return Optional.empty();
        }

        Map<String, Object> data = optContext.get().getContextData();
        if (data == null || data.isEmpty()) {
            return Optional.of(ConversationMemory.empty());
        }

        return Optional.of(parseMemoryFromData(data));
    }

    @Transactional
    public Optional<ContextualEntity> getEntity(Long userId, String chatId, ContextType type) {
        if (type == null) {
            return Optional.empty();
        }

        Optional<ConversationMemory> memoryOpt = getMemory(userId, chatId);
        if (memoryOpt.isPresent()) {
            Optional<ContextualEntity> entityOpt = memoryOpt.get().getEntity(type);
            if (entityOpt.isPresent()) {
                log.debug("Context hit for user {} conversation {}: found entity {}", userId, chatId, type);
                return entityOpt;
            }
        }

        // Fallback: check latest context across user conversations if not found in specific conversation
        Optional<ConversationContext> latestContext = getLatestContext(userId);
        if (latestContext.isPresent() && latestContext.get().getContextData() != null) {
            ConversationMemory latestMemory = parseMemoryFromData(latestContext.get().getContextData());
            Optional<ContextualEntity> fallbackOpt = latestMemory.getEntity(type);
            if (fallbackOpt.isPresent()) {
                log.debug("Fallback context hit from latest conversation for user {}: found entity {}", userId, type);
                return fallbackOpt;
            }
        }

        log.debug("Context miss for user {} conversation {} entity type {}", userId, chatId, type);
        return Optional.empty();
    }

    @Transactional
    public void recordEntity(Long userId, String chatId, ContextualEntity entity) {
        if (userId == null || chatId == null || chatId.isBlank() || entity == null) {
            return;
        }
        recordTurn(userId, chatId, null, null, List.of(entity));
    }

    @Transactional
    public void recordTurn(Long userId, String chatId, String userMessage, String assistantResponse, List<ContextualEntity> newEntities) {
        if (userId == null || chatId == null || chatId.isBlank()) {
            return;
        }

        String trimmedChatId = chatId.trim();
        ConversationContext context = repository.findByUserIdAndChatId(userId, trimmedChatId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    return ConversationContext.builder()
                            .user(user)
                            .chatId(trimmedChatId)
                            .contextType(ContextType.LAST_TASK)
                            .contextData(new LinkedHashMap<>())
                            .build();
                });

        Map<String, Object> data = context.getContextData() != null
                ? new LinkedHashMap<>(context.getContextData())
                : new LinkedHashMap<>();

        // 1. Update Entities
        Map<String, Object> entitiesMap = data.containsKey(KEY_ENTITIES) && data.get(KEY_ENTITIES) instanceof Map<?, ?>
                ? new LinkedHashMap<>((Map<String, Object>) data.get(KEY_ENTITIES))
                : new LinkedHashMap<>();

        if (newEntities != null && !newEntities.isEmpty()) {
            for (ContextualEntity entity : newEntities) {
                if (entity != null && entity.entityType() != null) {
                    Map<String, Object> entityMap = objectMapper.convertValue(entity, new TypeReference<Map<String, Object>>() {});
                    entitiesMap.put(entity.entityType().name(), entityMap);
                    context.setContextType(entity.entityType());

                    // Maintain backward compatibility with flat keys in contextData
                    if (entity.entityId() != null) {
                        data.put("id", entity.entityId());
                        data.put("taskId", entity.entityId());
                    }
                    if (entity.title() != null) {
                        data.put("title", entity.title());
                    }
                }
            }
        }
        data.put(KEY_ENTITIES, entitiesMap);

        // 2. Update Messages (bounded history)
        List<Map<String, Object>> messagesList = data.containsKey(KEY_MESSAGES) && data.get(KEY_MESSAGES) instanceof List<?>
                ? new ArrayList<>((List<Map<String, Object>>) data.get(KEY_MESSAGES))
                : new ArrayList<>();

        if (userMessage != null && !userMessage.isBlank()) {
            messagesList.add(objectMapper.convertValue(ConversationMessage.user(userMessage.trim()), new TypeReference<Map<String, Object>>() {}));
        }
        if (assistantResponse != null && !assistantResponse.isBlank()) {
            messagesList.add(objectMapper.convertValue(ConversationMessage.assistant(assistantResponse.trim()), new TypeReference<Map<String, Object>>() {}));
        }

        // Bound message history
        if (messagesList.size() > maxHistoryMessages) {
            int toRemove = messagesList.size() - maxHistoryMessages;
            log.debug("Trimming bounded message history for user {} conversation {} by {} messages", userId, chatId, toRemove);
            messagesList = new ArrayList<>(messagesList.subList(toRemove, messagesList.size()));
        }
        data.put(KEY_MESSAGES, messagesList);

        context.setContextData(data);
        context.setExpiresAt(ZonedDateTime.now().plusMinutes(ttlMinutes));

        repository.save(context);
        log.debug("Recorded conversation turn for user {} conversation {}. Entities count: {}, Messages count: {}",
                userId, trimmedChatId, entitiesMap.size(), messagesList.size());
    }

    @Transactional(readOnly = true)
    public String formatContextForPrompt(Long userId, String chatId) {
        if (userId == null || chatId == null || chatId.isBlank()) {
            return "";
        }

        Optional<ConversationContext> optContext = repository.findByUserIdAndChatId(userId, chatId.trim());
        if (optContext.isEmpty() || optContext.get().getExpiresAt().isBefore(ZonedDateTime.now())) {
            return "";
        }

        ConversationMemory memory = parseMemoryFromData(optContext.get().getContextData());
        StringBuilder sb = new StringBuilder();

        if (!memory.entities().isEmpty()) {
            sb.append("Recent Structured Entities:\n");
            for (Map.Entry<ContextType, ContextualEntity> entry : memory.entities().entrySet()) {
                ContextualEntity e = entry.getValue();
                sb.append("- ").append(entry.getKey()).append(": \"").append(e.title()).append("\"");
                if (e.platform() != null) {
                    sb.append(" (Platform: ").append(e.platform()).append(")");
                }
                if (e.entityId() != null) {
                    sb.append(" (ID: ").append(e.entityId()).append(")");
                }
                if (e.metadata() != null && !e.metadata().isEmpty()) {
                    sb.append(" Metadata: ").append(e.metadata());
                }
                sb.append("\n");
            }
        }

        if (!memory.recentMessages().isEmpty()) {
            sb.append("\nRecent Conversation History:\n");
            for (ConversationMessage msg : memory.recentMessages()) {
                sb.append(msg.role().equalsIgnoreCase("user") ? "User: " : "Assistant: ")
                  .append(msg.content())
                  .append("\n");
            }
        }

        return sb.toString().trim();
    }

    @Transactional
    public void updateContext(Long userId, String chatId, ContextType type, Map<String, Object> data) {
        if (userId == null || chatId == null || chatId.isBlank()) {
            return;
        }

        String trimmedChatId = chatId.trim();
        ConversationContext context = repository.findByUserIdAndChatId(userId, trimmedChatId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    return ConversationContext.builder()
                            .user(user)
                            .chatId(trimmedChatId)
                            .build();
                });

        context.setContextType(type);

        Map<String, Object> mergedData = context.getContextData() != null
                ? new LinkedHashMap<>(context.getContextData())
                : new LinkedHashMap<>();

        if (data != null) {
            mergedData.putAll(data);
        }

        // Sync into structured entities map as well
        if (type != null && type != ContextType.PENDING_CLARIFICATION && data != null) {
            Map<String, Object> entitiesMap = mergedData.containsKey(KEY_ENTITIES) && mergedData.get(KEY_ENTITIES) instanceof Map<?, ?>
                    ? new LinkedHashMap<>((Map<String, Object>) mergedData.get(KEY_ENTITIES))
                    : new LinkedHashMap<>();

            Long entityId = null;
            if (data.containsKey("id")) {
                try { entityId = Long.parseLong(data.get("id").toString()); } catch (Exception ignored) {}
            } else if (data.containsKey("taskId")) {
                try { entityId = Long.parseLong(data.get("taskId").toString()); } catch (Exception ignored) {}
            }

            String title = data.get("title") != null ? data.get("title").toString() : null;
            String platform = data.get("platform") != null ? data.get("platform").toString() : null;

            ContextualEntity contextualEntity = new ContextualEntity(
                    type,
                    entityId,
                    title,
                    platform,
                    Instant.now(),
                    trimmedChatId,
                    data
            );
            entitiesMap.put(type.name(), objectMapper.convertValue(contextualEntity, new TypeReference<Map<String, Object>>() {}));
            mergedData.put(KEY_ENTITIES, entitiesMap);
        }

        context.setContextData(mergedData);
        context.setExpiresAt(ZonedDateTime.now().plusMinutes(ttlMinutes));

        repository.save(context);
        log.debug("Updated context for user {} and chat {} to type {}", userId, trimmedChatId, type);
    }

    @Transactional
    public void clearContext(Long userId, String chatId) {
        if (userId != null && chatId != null && !chatId.isBlank()) {
            repository.deleteByUserIdAndChatId(userId, chatId.trim());
            log.debug("Cleared context for user {} and chat {}", userId, chatId);
        }
    }

    private ConversationMemory parseMemoryFromData(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return ConversationMemory.empty();
        }

        Map<ContextType, ContextualEntity> entities = new EnumMap<>(ContextType.class);
        if (data.containsKey(KEY_ENTITIES) && data.get(KEY_ENTITIES) instanceof Map<?, ?> rawEntities) {
            for (Map.Entry<?, ?> entry : rawEntities.entrySet()) {
                try {
                    ContextType type = ContextType.valueOf(entry.getKey().toString());
                    ContextualEntity entity = objectMapper.convertValue(entry.getValue(), ContextualEntity.class);
                    entities.put(type, entity);
                } catch (Exception e) {
                    log.warn("Failed to deserialize ContextualEntity for key {}", entry.getKey(), e);
                }
            }
        }

        // Backward compatibility: if flat keys exist but entities map is empty, synthesize contextual entity
        if (entities.isEmpty() && data.containsKey("id")) {
            try {
                Long id = Long.parseLong(data.get("id").toString());
                String title = data.get("title") != null ? data.get("title").toString() : null;
                entities.put(ContextType.LAST_TASK, ContextualEntity.task(id, title, null, data));
            } catch (Exception ignored) {}
        }

        List<ConversationMessage> messages = new ArrayList<>();
        if (data.containsKey(KEY_MESSAGES) && data.get(KEY_MESSAGES) instanceof List<?> rawMessages) {
            for (Object obj : rawMessages) {
                try {
                    ConversationMessage msg = objectMapper.convertValue(obj, ConversationMessage.class);
                    messages.add(msg);
                } catch (Exception e) {
                    log.warn("Failed to deserialize ConversationMessage", e);
                }
            }
        }

        return new ConversationMemory(entities, messages);
    }
}
