package com.devcommand.devcommand.integrations.ai.rag;

import com.devcommand.devcommand.integrations.ai.rag.config.RagConfig;
import com.devcommand.devcommand.integrations.ai.rag.config.RagProperties;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentRequest;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeSearchRequest;
import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeVisibility;
import com.devcommand.devcommand.integrations.ai.rag.repository.KnowledgeDocumentRepository;
import com.devcommand.devcommand.integrations.ai.rag.service.DefaultRagKnowledgeService;
import com.devcommand.devcommand.integrations.ai.rag.service.DocumentChunker;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagProductionHardeningTest {

    private static Validator validator;

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private JdbcTemplate jdbcTemplate;

    private DefaultRagKnowledgeService service;
    private RagProperties properties;
    private User testUser;

    @BeforeAll
    static void initValidator() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.setTopK(4);
        properties.setSimilarityThreshold(0.65);
        properties.setChunkSize(300);
        properties.setChunkOverlap(30);

        DocumentChunker chunker = new DocumentChunker(properties);
        service = new DefaultRagKnowledgeService(
                documentRepository,
                userRepository,
                vectorStore,
                chunker,
                properties,
                jdbcTemplate
        );

        testUser = new User();
        testUser.setId(100L);
        testUser.setEmail("dev@devcommand.com");
        lenient().when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
    }

    @Test
    @DisplayName("Oversized content (> 100,000 chars) is rejected by service boundary validation")
    void oversizedContent_rejectedByService() {
        String giantContent = "A".repeat(100_001);
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "doc-1", "Valid Title", giantContent, DocumentType.ARCHITECTURE, "source", KnowledgeVisibility.PRIVATE, null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.ingestDocument(100L, request)
        );
        assertThat(ex.getMessage()).contains("100,000 characters");
    }

    @Test
    @DisplayName("Oversized title (> 255 chars) is rejected by service boundary validation")
    void oversizedTitle_rejectedByService() {
        String giantTitle = "T".repeat(256);
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "doc-1", giantTitle, "Normal content", DocumentType.ARCHITECTURE, "source", KnowledgeVisibility.PRIVATE, null
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                service.ingestDocument(100L, request)
        );
        assertThat(ex.getMessage()).contains("255 characters");
    }

    @Test
    @DisplayName("Non-admin user cannot ingest SYSTEM visibility documents")
    void nonAdminUser_cannotIngestSystemDocument() {
        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "doc-sys", "System Architecture", "System content", DocumentType.ARCHITECTURE, "src", KnowledgeVisibility.SYSTEM, null
        );

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                service.ingestDocument(100L, request)
        );
        assertThat(ex.getMessage()).contains("authorized system accounts");
    }

    @Test
    @DisplayName("Embedding failure during ingestion throws sanitized IllegalStateException")
    void embeddingFailure_throwsSanitizedIllegalStateException() {
        doThrow(new RuntimeException("OpenAI quota exceeded / timeout"))
                .when(vectorStore).add(any());

        KnowledgeDocumentRequest request = new KnowledgeDocumentRequest(
                "doc-fail", "Architecture Doc", "Some architecture overview", DocumentType.ARCHITECTURE, "src", KnowledgeVisibility.PRIVATE, null
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                service.ingestDocument(100L, request)
        );
        assertThat(ex.getMessage()).contains("Failed to generate embeddings");
        assertThat(ex.getMessage()).doesNotContain("quota"); // Sanitized message
    }

    @Test
    @DisplayName("Search query length is sanitized and bounded to 1,000 characters")
    void searchQuery_sanitizedAndBounded() {
        String longQuery = "devcommand architecture ".repeat(100); // > 2,000 chars

        service.searchKnowledge(100L, longQuery, DocumentType.ARCHITECTURE, 5);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest req = captor.getValue();
        assertThat(req.getQuery().length()).isLessThanOrEqualTo(1000);
    }

    @Test
    @DisplayName("Search topK parameter is clamped to server-side upper limit of 20")
    void searchTopK_clampedToLimit() {
        service.searchKnowledge(100L, "authentication", null, 100);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());

        SearchRequest req = captor.getValue();
        assertThat(req.getTopK()).isEqualTo(20);
    }

    @Test
    @DisplayName("RagConfig refuses to use mock embeddings in production profile if OPENAI_API_KEY is missing")
    void ragConfig_productionProfileRequiresOpenAiKey() {
        RagConfig config = new RagConfig();
        ReflectionTestUtils.setField(config, "openAiApiKey", "dummy-key");
        ReflectionTestUtils.setField(config, "activeProfile", "prod");

        RagProperties props = new RagProperties();
        props.setEmbeddingProvider("openai");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                config.embeddingModel(props)
        );
        assertThat(ex.getMessage()).contains("Production environment requires OPENAI_API_KEY");
    }

    @Test
    @DisplayName("Jakarta Bean Validation enforces size limits on KnowledgeDocumentRequest and KnowledgeSearchRequest")
    void beanValidation_enforcesSizeLimits() {
        KnowledgeDocumentRequest oversizedDoc = new KnowledgeDocumentRequest(
                "id", "Title", "A".repeat(100_001), DocumentType.ARCHITECTURE, null, null, null
        );
        Set<ConstraintViolation<KnowledgeDocumentRequest>> docViolations = validator.validate(oversizedDoc);
        assertThat(docViolations).anyMatch(v -> v.getPropertyPath().toString().equals("content"));

        KnowledgeSearchRequest oversizedSearch = new KnowledgeSearchRequest(
                "Q".repeat(1001), null, 25
        );
        Set<ConstraintViolation<KnowledgeSearchRequest>> searchViolations = validator.validate(oversizedSearch);
        assertThat(searchViolations).anyMatch(v -> v.getPropertyPath().toString().equals("query"));
        assertThat(searchViolations).anyMatch(v -> v.getPropertyPath().toString().equals("topK"));
    }
}
