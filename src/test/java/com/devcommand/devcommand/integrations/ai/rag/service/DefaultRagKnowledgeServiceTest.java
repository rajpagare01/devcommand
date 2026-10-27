package com.devcommand.devcommand.integrations.ai.rag.service;

import com.devcommand.devcommand.integrations.ai.rag.config.RagProperties;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentRequest;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentResponse;
import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeDocument;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeQueryResult;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeVisibility;
import com.devcommand.devcommand.integrations.ai.rag.repository.KnowledgeDocumentRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultRagKnowledgeServiceTest {

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DocumentChunker documentChunker;
    private RagProperties ragProperties;
    private DefaultRagKnowledgeService service;

    private User testUser;

    @BeforeEach
    void setUp() {
        ragProperties = new RagProperties();
        ragProperties.setTopK(4);
        ragProperties.setSimilarityThreshold(0.65);
        ragProperties.setChunkSize(200);
        ragProperties.setChunkOverlap(20);

        documentChunker = new DocumentChunker(ragProperties);
        service = new DefaultRagKnowledgeService(
                documentRepository,
                userRepository,
                vectorStore,
                documentChunker,
                ragProperties,
                jdbcTemplate
        );

        testUser = new User();
        testUser.setId(10L);
        testUser.setEmail("developer@example.com");

        lenient().when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Ingest valid private document indexes chunks and saves entity")
    void ingestDocument_valid_success() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(testUser));
        when(documentRepository.findByDocumentIdAndOwnerId("auth-doc", 10L)).thenReturn(Optional.empty());
        when(documentRepository.save(any(KnowledgeDocument.class))).thenAnswer(inv -> {
            KnowledgeDocument doc = inv.getArgument(0);
            doc.setId(1L);
            return doc;
        });

        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "auth-doc",
                "Authentication Architecture",
                "DevCommand uses JWT tokens stored in Authorization header.",
                DocumentType.ARCHITECTURE,
                "docs/auth.md",
                KnowledgeVisibility.PRIVATE,
                "security,jwt"
        );

        KnowledgeDocumentResponse response = service.ingestDocument(10L, request);

        assertNotNull(response);
        assertEquals("auth-doc", response.documentId());
        assertEquals("Authentication Architecture", response.title());
        assertEquals(KnowledgeVisibility.PRIVATE, response.visibility());
        assertEquals(10L, response.ownerId());
        assertTrue(response.chunkCount() > 0);

        verify(vectorStore, times(1)).add(anyList());
        verify(documentRepository, times(1)).save(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("Ingest invalid document throws IllegalArgumentException")
    void ingestDocument_invalid_throws() {
        assertThrows(IllegalArgumentException.class, () ->
                service.ingestDocument(10L, null));

        assertThrows(IllegalArgumentException.class, () ->
                service.ingestDocument(10L, new KnowledgeDocumentRequest("id", "", "content", DocumentType.ARCHITECTURE, null, null, null)));

        assertThrows(IllegalArgumentException.class, () ->
                service.ingestDocument(10L, new KnowledgeDocumentRequest("id", "Title", "", DocumentType.ARCHITECTURE, null, null, null)));

        assertThrows(IllegalArgumentException.class, () ->
                service.ingestDocument(10L, new KnowledgeDocumentRequest("id", "Title", "Content", null, null, null, null)));
    }

    @Test
    @DisplayName("Security: Normal user cannot create SYSTEM visibility document")
    void ingestDocument_normalUserCreatesSystemDoc_rejected() {
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "sys-doc",
                "System Spec",
                "System internals",
                DocumentType.ARCHITECTURE,
                null,
                KnowledgeVisibility.SYSTEM,
                null
        );

        assertThrows(AccessDeniedException.class, () -> service.ingestDocument(10L, request));
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    @DisplayName("Idempotent ingestion: repeated ingestion with same checksum skips re-indexing")
    void ingestDocument_duplicateContent_skipsReindexing() {
        String content = "DevCommand uses JWT tokens stored in Authorization header.";
        String checksum = documentChunker.calculateChecksum(documentChunker.normalizeContent(content));

        KnowledgeDocument existing = KnowledgeDocument.builder()
                .id(1L)
                .documentId("auth-doc")
                .title("Authentication Architecture")
                .content(content)
                .documentType(DocumentType.ARCHITECTURE)
                .owner(testUser)
                .visibility(KnowledgeVisibility.PRIVATE)
                .checksum(checksum)
                .chunkCount(1)
                .build();

        when(documentRepository.findByDocumentIdAndOwnerId("auth-doc", 10L)).thenReturn(Optional.of(existing));

        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "auth-doc",
                "Authentication Architecture",
                content,
                DocumentType.ARCHITECTURE,
                null,
                KnowledgeVisibility.PRIVATE,
                null
        );

        KnowledgeDocumentResponse response = service.ingestDocument(10L, request);

        assertNotNull(response);
        assertEquals("auth-doc", response.documentId());
        // Verify vector store was NOT called (no duplicate chunks created)
        verify(vectorStore, never()).add(anyList());
        verify(documentRepository, never()).save(any(KnowledgeDocument.class));
    }

    @Test
    @DisplayName("Re-indexing: updated content replaces old vector chunks and updates entity")
    void ingestDocument_modifiedContent_reindexes() {
        String oldContent = "Old content";
        String oldChecksum = documentChunker.calculateChecksum(oldContent);

        KnowledgeDocument existing = KnowledgeDocument.builder()
                .id(1L)
                .documentId("auth-doc")
                .title("Old Title")
                .content(oldContent)
                .documentType(DocumentType.ARCHITECTURE)
                .owner(testUser)
                .visibility(KnowledgeVisibility.PRIVATE)
                .checksum(oldChecksum)
                .chunkCount(1)
                .build();

        when(documentRepository.findByDocumentIdAndOwnerId("auth-doc", 10L)).thenReturn(Optional.of(existing));
        when(documentRepository.save(any(KnowledgeDocument.class))).thenAnswer(inv -> inv.getArgument(0));

        String newContent = "Updated content explaining new architecture";
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "auth-doc",
                "New Title",
                newContent,
                DocumentType.ARCHITECTURE,
                "docs/new.md",
                KnowledgeVisibility.PRIVATE,
                null
        );

        KnowledgeDocumentResponse response = service.ingestDocument(10L, request);

        assertEquals("New Title", response.title());
        assertNotEquals(oldChecksum, response.checksum());

        // Verify old chunks deleted from vector store
        verify(jdbcTemplate, times(1)).update(anyString(), eq("auth-doc"), eq("10"));
        // Verify new chunks added
        verify(vectorStore, times(1)).add(anyList());
        verify(documentRepository, times(1)).save(existing);
    }

    @Test
    @DisplayName("Search knowledge filters by user authorization and metadata")
    void searchKnowledge_appliesFilterAndReturnsResults() {
        Document matchDoc = new Document(
                "chunk-1",
                "DevCommand uses stateless JWT authentication.",
                Map.of(
                        "documentId", "auth-doc",
                        "title", "Auth Architecture",
                        "source", "docs/auth.md",
                        "documentType", "ARCHITECTURE",
                        "visibility", "PRIVATE",
                        "ownerId", 10L
                )
        );

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(matchDoc));

        List<KnowledgeQueryResult> results = service.searchKnowledge(10L, "how does auth work", DocumentType.ARCHITECTURE, 3);

        assertEquals(1, results.size());
        KnowledgeQueryResult res = results.get(0);
        assertEquals("auth-doc", res.documentId());
        assertEquals("Auth Architecture", res.title());
        assertEquals("docs/auth.md", res.source());
        assertEquals(DocumentType.ARCHITECTURE, res.documentType());
        assertEquals("DevCommand uses stateless JWT authentication.", res.content());

        ArgumentCaptor<SearchRequest> reqCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(reqCaptor.capture());

        SearchRequest captured = reqCaptor.getValue();
        assertEquals("how does auth work", captured.getQuery());
        assertEquals(3, captured.getTopK());
        assertTrue(captured.hasFilterExpression());
    }

    @Test
    @DisplayName("Search knowledge handles empty query and store exceptions gracefully")
    void searchKnowledge_errorHandling() {
        assertTrue(service.searchKnowledge(10L, "", null, null).isEmpty());
        assertTrue(service.searchKnowledge(10L, "   ", null, null).isEmpty());

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenThrow(new RuntimeException("PGVector timeout"));
        List<KnowledgeQueryResult> results = service.searchKnowledge(10L, "test query", null, null);
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    @DisplayName("Delete document deletes JPA record and vector chunks")
    void deleteDocument_removesEntityAndVectors() {
        service.deleteDocument(10L, "doc-to-delete");

        verify(documentRepository, times(1)).deleteByDocumentIdAndOwnerId("doc-to-delete", 10L);
        verify(jdbcTemplate, times(1)).update(anyString(), eq("doc-to-delete"), eq("10"));
    }
}
