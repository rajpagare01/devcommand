package com.devcommand.devcommand.conversation.service;

import com.devcommand.devcommand.conversation.entity.ConversationContext;
import com.devcommand.devcommand.conversation.model.ContextType;
import com.devcommand.devcommand.conversation.repository.ConversationContextRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

import com.devcommand.devcommand.integration.AbstractIntegrationTest;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
class ConversationContextServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ConversationContextService contextService;

    @Autowired
    private ConversationContextRepository contextRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private User testUser2;
    private final String CHAT_ID_1 = "chat1";
    private final String CHAT_ID_2 = "chat2";

    @BeforeEach
    void setUp() {
        contextRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User();
        testUser.setName("testuser_context");
        testUser.setEmail("context@example.com");
        testUser.setPassword("password");
        testUser = userRepository.save(testUser);

        testUser2 = new User();
        testUser2.setName("testuser_context2");
        testUser2.setEmail("context2@example.com");
        testUser2.setPassword("password");
        testUser2 = userRepository.save(testUser2);
    }

    @Test
    void testUpdateAndRetrieveContext() {
        Map<String, Object> data = Map.of("taskId", 100);

        contextService.updateContext(testUser.getId(), CHAT_ID_1, ContextType.LAST_TASK, data);

        Optional<ConversationContext> retrieved = contextService.getContext(testUser.getId(), CHAT_ID_1);
        
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getContextType()).isEqualTo(ContextType.LAST_TASK);
        assertThat(retrieved.get().getContextData()).containsEntry("taskId", 100);
    }

    @Test
    void testContextIsIsolatedByUserAndChat() {
        Map<String, Object> data1 = Map.of("taskId", 100);
        contextService.updateContext(testUser.getId(), CHAT_ID_1, ContextType.LAST_TASK, data1);

        // Different user, same chat
        Optional<ConversationContext> wrongUser = contextService.getContext(testUser2.getId(), CHAT_ID_1);
        assertThat(wrongUser).isEmpty();

        // Same user, different chat
        Optional<ConversationContext> wrongChat = contextService.getContext(testUser.getId(), CHAT_ID_2);
        assertThat(wrongChat).isEmpty();
    }

    @Test
    void testClearContext() {
        contextService.updateContext(testUser.getId(), CHAT_ID_1, ContextType.LAST_TASK, Map.of("k", "v"));
        assertThat(contextService.getContext(testUser.getId(), CHAT_ID_1)).isPresent();

        contextService.clearContext(testUser.getId(), CHAT_ID_1);

        assertThat(contextService.getContext(testUser.getId(), CHAT_ID_1)).isEmpty();
    }
}
