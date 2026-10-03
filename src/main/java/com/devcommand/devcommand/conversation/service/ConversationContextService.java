package com.devcommand.devcommand.conversation.service;

import com.devcommand.devcommand.conversation.entity.ConversationContext;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.repository.ConversationContextRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationContextService {
    private final ConversationContextRepository repository;
    private final UserRepository userRepository;

    @Value("${devcommand.conversation.ttl-minutes:30}")
    private long ttlMinutes;

    @Transactional
    public Optional<ConversationContext> getContext(Long userId, String chatId) {
        Optional<ConversationContext> optContext = repository.findByUserIdAndChatId(userId, chatId);
        if (optContext.isEmpty()) {
            return Optional.empty();
        }

        ConversationContext context = optContext.get();
        if (context.getExpiresAt().isBefore(ZonedDateTime.now())) {
            log.debug("Found expired context for user {} and chat {}, deleting", userId, chatId);
            repository.delete(context);
            return Optional.empty();
        }

        return Optional.of(context);
    }

    @Transactional
    public void updateContext(Long userId, String chatId, ContextType type, Map<String, Object> data) {
        ConversationContext context = repository.findByUserIdAndChatId(userId, chatId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
                    return ConversationContext.builder()
                            .user(user)
                            .chatId(chatId)
                            .build();
                });
        
        context.setContextType(type);
        context.setContextData(data);
        context.setExpiresAt(ZonedDateTime.now().plusMinutes(ttlMinutes));
        
        repository.save(context);
        log.debug("Updated context for user {} and chat {} to type {}", userId, chatId, type);
    }

    @Transactional
    public void clearContext(Long userId, String chatId) {
        repository.deleteByUserIdAndChatId(userId, chatId);
        log.debug("Cleared context for user {} and chat {}", userId, chatId);
    }
}
