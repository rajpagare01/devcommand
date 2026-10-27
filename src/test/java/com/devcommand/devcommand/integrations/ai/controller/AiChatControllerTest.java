package com.devcommand.devcommand.integrations.ai.controller;

import com.devcommand.devcommand.conversation.service.ConversationContextService;
import com.devcommand.devcommand.exception.GlobalExceptionHandler;
import com.devcommand.devcommand.integrations.ai.dto.AiChatRequest;
import com.devcommand.devcommand.integrations.ai.orchestrator.AiOrchestrator;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorRequest;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorResponse;
import com.devcommand.devcommand.security.UserPrincipal;
import com.devcommand.devcommand.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AiChatController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AiChatControllerTest {

    private static final Long CALLER_ID = 1L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private AiOrchestrator orchestrator;
    @MockBean private ConversationContextService contextService;
    @MockBean private com.devcommand.devcommand.security.JwtService jwtService;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean private com.devcommand.devcommand.ratelimit.service.RateLimitService rateLimitService;
    @MockBean private com.devcommand.devcommand.metrics.DevCommandMetrics devCommandMetrics;

    @BeforeEach
    void setUpSecurityContext() {
        User user = new User();
        user.setId(CALLER_ID);
        user.setEmail("user1@example.com");
        user.setPassword("hashedpassword");

        UserPrincipal principal = new UserPrincipal(user);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Multi-turn request preserves and returns client conversationId")
    void testChatWithExistingConversationId() throws Exception {
        String convId = "client-conversation-123";
        AiChatRequest request = new AiChatRequest("Create a task to revise Two Sum tomorrow", convId, null);

        when(contextService.isOwnedByAnotherUser(convId, CALLER_ID)).thenReturn(false);
        when(orchestrator.process(any(OrchestratorRequest.class)))
                .thenReturn(OrchestratorResponse.executed("Created task", convId, List.of()));

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTED"))
                .andExpect(jsonPath("$.conversationId").value(convId));

        ArgumentCaptor<OrchestratorRequest> captor = ArgumentCaptor.forClass(OrchestratorRequest.class);
        verify(orchestrator).process(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(CALLER_ID);
        assertThat(captor.getValue().conversationId()).isEqualTo(convId);
    }

    @Test
    @DisplayName("Absent conversationId establishes a new unique conversation context")
    void testChatWithoutConversationIdGeneratesNewOne() throws Exception {
        AiChatRequest request = new AiChatRequest("I solved Two Sum today", null, null);

        when(orchestrator.process(any(OrchestratorRequest.class)))
                .thenAnswer(inv -> {
                    OrchestratorRequest req = inv.getArgument(0);
                    return OrchestratorResponse.executed("Logged Two Sum", req.conversationId(), List.of());
                });

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").isNotEmpty());

        ArgumentCaptor<OrchestratorRequest> captor = ArgumentCaptor.forClass(OrchestratorRequest.class);
        verify(orchestrator).process(captor.capture());
        assertThat(captor.getValue().conversationId()).isNotBlank();
    }

    @Test
    @DisplayName("Security violation: User A cannot use User B's conversationId -> 403 Forbidden")
    void testCrossUserConversationAccessRejected() throws Exception {
        String victimConvId = "user-b-private-conversation";
        AiChatRequest maliciousRequest = new AiChatRequest("Show my tasks", victimConvId, null);

        // Security check: conversation belongs to another user
        when(contextService.isOwnedByAnotherUser(victimConvId, CALLER_ID)).thenReturn(true);

        mockMvc.perform(post("/api/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(maliciousRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("ERROR"))
                .andExpect(jsonPath("$.message").value("Access denied: Conversation belongs to another user."));

        // Orchestrator must NEVER be called
        verify(orchestrator, never()).process(any());
    }
}
