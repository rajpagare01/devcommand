package com.devcommand.devcommand.integrations.ai.rag.controller;

import com.devcommand.devcommand.exception.GlobalExceptionHandler;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentRequest;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentResponse;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeSearchRequest;
import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeQueryResult;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeVisibility;
import com.devcommand.devcommand.integrations.ai.rag.service.RagKnowledgeService;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = KnowledgeIngestionController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class KnowledgeIngestionControllerTest {

    private static final Long CALLER_ID = 42L;

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockBean private RagKnowledgeService ragKnowledgeService;
    @MockBean private com.devcommand.devcommand.security.JwtService jwtService;
    @MockBean private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;
    @MockBean private com.devcommand.devcommand.ratelimit.service.RateLimitService rateLimitService;
    @MockBean private com.devcommand.devcommand.metrics.DevCommandMetrics devCommandMetrics;

    @BeforeEach
    void setUpSecurityContext() {
        User user = new User();
        user.setId(CALLER_ID);
        user.setEmail("ragtester@example.com");
        user.setPassword("secret");

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
    @DisplayName("POST /api/ai/knowledge/documents creates document with owner derived from principal")
    void ingestDocument_success() throws Exception {
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "jwt-arch",
                "JWT Architecture",
                "DevCommand uses HMAC-SHA256 tokens.",
                DocumentType.ARCHITECTURE,
                "docs/jwt.md",
                KnowledgeVisibility.PRIVATE,
                "auth,jwt"
        );

        KnowledgeDocumentResponse response = new KnowledgeDocumentResponse(
                1L,
                "jwt-arch",
                "JWT Architecture",
                DocumentType.ARCHITECTURE,
                "docs/jwt.md",
                CALLER_ID,
                KnowledgeVisibility.PRIVATE,
                "auth,jwt",
                "checksum123",
                1,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(ragKnowledgeService.ingestDocument(eq(CALLER_ID), any(KnowledgeDocumentRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/ai/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.documentId").value("jwt-arch"))
                .andExpect(jsonPath("$.title").value("JWT Architecture"))
                .andExpect(jsonPath("$.ownerId").value(CALLER_ID));

        ArgumentCaptor<Long> ownerCaptor = ArgumentCaptor.forClass(Long.class);
        verify(ragKnowledgeService).ingestDocument(ownerCaptor.capture(), any(KnowledgeDocumentRequest.class));
        assertEquals(CALLER_ID, ownerCaptor.getValue());
    }

    @Test
    @DisplayName("GET /api/ai/knowledge/documents returns paginated list")
    void listDocuments_success() throws Exception {
        KnowledgeDocumentResponse doc = new KnowledgeDocumentResponse(
                1L, "doc-1", "Doc 1", DocumentType.USER_NOTE, null, CALLER_ID, KnowledgeVisibility.PRIVATE, null, "hash", 1, LocalDateTime.now(), LocalDateTime.now()
        );

        when(ragKnowledgeService.listDocuments(eq(CALLER_ID), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doc)));

        mockMvc.perform(get("/api/ai/knowledge/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].documentId").value("doc-1"));
    }

    @Test
    @DisplayName("GET /api/ai/knowledge/documents/{id} returns single document if found")
    void getDocument_found() throws Exception {
        KnowledgeDocumentResponse doc = new KnowledgeDocumentResponse(
                1L, "doc-1", "Doc 1", DocumentType.USER_NOTE, null, CALLER_ID, KnowledgeVisibility.PRIVATE, null, "hash", 1, LocalDateTime.now(), LocalDateTime.now()
        );

        when(ragKnowledgeService.getDocument(CALLER_ID, "doc-1")).thenReturn(Optional.of(doc));

        mockMvc.perform(get("/api/ai/knowledge/documents/doc-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value("doc-1"));
    }

    @Test
    @DisplayName("DELETE /api/ai/knowledge/documents/{id} returns 204")
    void deleteDocument_success() throws Exception {
        mockMvc.perform(delete("/api/ai/knowledge/documents/doc-1"))
                .andExpect(status().isNoContent());

        verify(ragKnowledgeService).deleteDocument(CALLER_ID, "doc-1");
    }

    @Test
    @DisplayName("POST /api/ai/knowledge/search returns query results")
    void search_success() throws Exception {
        KnowledgeSearchRequest searchReq = new KnowledgeSearchRequest("auth", DocumentType.ARCHITECTURE, 5);
        KnowledgeQueryResult res = new KnowledgeQueryResult(
                "doc-1", "Title", "src", DocumentType.ARCHITECTURE, KnowledgeVisibility.SYSTEM, null, "Content", 0.9, Map.of()
        );

        when(ragKnowledgeService.searchKnowledge(CALLER_ID, "auth", DocumentType.ARCHITECTURE, 5))
                .thenReturn(List.of(res));

        mockMvc.perform(post("/api/ai/knowledge/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(searchReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value("doc-1"));
    }
}
