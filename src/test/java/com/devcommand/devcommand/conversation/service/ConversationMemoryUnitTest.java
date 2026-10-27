package com.devcommand.devcommand.conversation.service;

import com.devcommand.devcommand.conversation.entity.ConversationContext;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.model.ContextualEntity;
import com.devcommand.devcommand.conversation.model.ConversationMemory;
import com.devcommand.devcommand.conversation.model.ConversationMessage;
import com.devcommand.devcommand.conversation.repository.ConversationContextRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryUnitTest {

    @Mock private ConversationContextRepository repository;
    @Mock private UserRepository userRepository;

    private ObjectMapper objectMapper;
    private ConversationContextService service;

    private final Long userId = 100L;
    private final String conversationId = "conv-abc-123";
    private User testUser;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        service = new ConversationContextService(repository, userRepository, objectMapper);
        ReflectionTestUtils.setField(service, "ttlMinutes", 30L);
        ReflectionTestUtils.setField(service, "maxHistoryMessages", 4);

        testUser = new User();
        testUser.setId(userId);
        testUser.setEmail("dev@devcommand.com");
    }

    @Test
    @DisplayName("Multiple contextual entities coexist simultaneously without overwriting")
    void testMultipleEntitiesCoexist() {
        ConversationContext context = ConversationContext.builder()
                .id(1L)
                .user(testUser)
                .chatId(conversationId)
                .contextType(ContextType.LAST_TASK)
                .contextData(new LinkedHashMap<>())
                .expiresAt(ZonedDateTime.now().plusMinutes(30))
                .build();

        when(repository.findByUserIdAndChatId(userId, conversationId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 1. Record DSA problem
        ContextualEntity dsaEntity = ContextualEntity.dsa(101L, "Two Sum", "LEETCODE", conversationId, Map.of("difficulty", "EASY"));
        service.recordTurn(userId, conversationId, "Solved Two Sum", "Logged Two Sum", List.of(dsaEntity));

        // 2. Record Task
        ContextualEntity taskEntity = ContextualEntity.task(202L, "Revise Two Sum", conversationId, Map.of("priority", "HIGH"));
        service.recordTurn(userId, conversationId, "Create revision task", "Created task", List.of(taskEntity));

        // Both must be retrievable
        Optional<ContextualEntity> retrievedDsa = service.getEntity(userId, conversationId, ContextType.LAST_DSA_PROBLEM);
        Optional<ContextualEntity> retrievedTask = service.getEntity(userId, conversationId, ContextType.LAST_TASK);

        assertThat(retrievedDsa).isPresent();
        assertThat(retrievedDsa.get().title()).isEqualTo("Two Sum");
        assertThat(retrievedDsa.get().entityId()).isEqualTo(101L);

        assertThat(retrievedTask).isPresent();
        assertThat(retrievedTask.get().title()).isEqualTo("Revise Two Sum");
        assertThat(retrievedTask.get().entityId()).isEqualTo(202L);
    }

    @Test
    @DisplayName("Message history is bounded by max-history-messages limit")
    void testBoundedMessageHistory() {
        ConversationContext context = ConversationContext.builder()
                .id(1L)
                .user(testUser)
                .chatId(conversationId)
                .contextType(ContextType.LAST_TASK)
                .contextData(new LinkedHashMap<>())
                .expiresAt(ZonedDateTime.now().plusMinutes(30))
                .build();

        when(repository.findByUserIdAndChatId(userId, conversationId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // maxHistoryMessages is configured to 4 in setUp()
        // Turn 1 (2 messages: user + assistant)
        service.recordTurn(userId, conversationId, "User 1", "Assistant 1", List.of());
        // Turn 2 (2 messages: user + assistant) -> total 4
        service.recordTurn(userId, conversationId, "User 2", "Assistant 2", List.of());
        // Turn 3 (2 messages: user + assistant) -> total would be 6, trimmed to 4
        service.recordTurn(userId, conversationId, "User 3", "Assistant 3", List.of());

        Optional<ConversationMemory> memory = service.getMemory(userId, conversationId);
        assertThat(memory).isPresent();
        List<ConversationMessage> msgs = memory.get().recentMessages();
        assertThat(msgs).hasSize(4);

        // The oldest messages ("User 1", "Assistant 1") should have been trimmed out
        assertThat(msgs.get(0).content()).isEqualTo("User 2");
        assertThat(msgs.get(1).content()).isEqualTo("Assistant 2");
        assertThat(msgs.get(2).content()).isEqualTo("User 3");
        assertThat(msgs.get(3).content()).isEqualTo("Assistant 3");
    }

    @Test
    @DisplayName("Expired context is purged and returns empty")
    void testExpiredContextPurged() {
        ConversationContext expiredContext = ConversationContext.builder()
                .id(1L)
                .user(testUser)
                .chatId(conversationId)
                .contextType(ContextType.LAST_TASK)
                .contextData(new LinkedHashMap<>())
                .expiresAt(ZonedDateTime.now().minusMinutes(5)) // expired 5 minutes ago
                .build();

        when(repository.findByUserIdAndChatId(userId, conversationId)).thenReturn(Optional.of(expiredContext));

        Optional<ConversationContext> result = service.getContext(userId, conversationId);

        assertThat(result).isEmpty();
        verify(repository).delete(expiredContext);
    }

    @Test
    @DisplayName("Detects conversation ownership violation across users")
    void testIsOwnedByAnotherUser() {
        when(repository.existsByChatIdAndUserIdNot("user-b-conv", userId)).thenReturn(true);
        when(repository.existsByChatIdAndUserIdNot("my-own-conv", userId)).thenReturn(false);

        boolean isViolating = service.isOwnedByAnotherUser("user-b-conv", userId);
        boolean isMine = service.isOwnedByAnotherUser("my-own-conv", userId);

        assertThat(isViolating).isTrue();
        assertThat(isMine).isFalse();
    }

    @Test
    @DisplayName("formatContextForPrompt formats structured entities and bounded history")
    void testFormatContextForPrompt() {
        ConversationContext context = ConversationContext.builder()
                .id(1L)
                .user(testUser)
                .chatId(conversationId)
                .contextType(ContextType.LAST_TASK)
                .contextData(new LinkedHashMap<>())
                .expiresAt(ZonedDateTime.now().plusMinutes(30))
                .build();

        when(repository.findByUserIdAndChatId(userId, conversationId)).thenReturn(Optional.of(context));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ContextualEntity dsaEntity = ContextualEntity.dsa(101L, "Two Sum", "LEETCODE", conversationId, Map.of("difficulty", "EASY"));
        service.recordTurn(userId, conversationId, "I solved Two Sum", "Logged Two Sum as Easy", List.of(dsaEntity));

        String promptContext = service.formatContextForPrompt(userId, conversationId);

        assertThat(promptContext).contains("Recent Structured Entities:");
        assertThat(promptContext).contains("LAST_DSA_PROBLEM: \"Two Sum\"");
        assertThat(promptContext).contains("LEETCODE");
        assertThat(promptContext).contains("User: I solved Two Sum");
        assertThat(promptContext).contains("Assistant: Logged Two Sum as Easy");
    }
}
