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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultRagKnowledgeService implements RagKnowledgeService {

    private final KnowledgeDocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final VectorStore vectorStore;
    private final DocumentChunker documentChunker;
    private final RagProperties ragProperties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public KnowledgeDocumentResponse ingestDocument(Long userId, KnowledgeDocumentRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Ingestion request cannot be null.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("Document title cannot be blank.");
        }
        if (request.title().length() > 255) {
            throw new IllegalArgumentException("Document title cannot exceed 255 characters.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Document content cannot be blank.");
        }
        if (request.content().length() > 100000) {
            throw new IllegalArgumentException("Document content exceeds maximum allowed size (100,000 characters).");
        }
        if (request.documentType() == null) {
            throw new IllegalArgumentException("Document type is required.");
        }

        KnowledgeVisibility visibility = request.visibility();
        if (visibility == null) {
            visibility = (userId != null) ? KnowledgeVisibility.PRIVATE : KnowledgeVisibility.SYSTEM;
        }

        // Security check: Only system/admin calls (userId == null or explicit admin) can publish SYSTEM documents
        if (visibility == KnowledgeVisibility.SYSTEM && userId != null) {
            // For now, non-system accounts cannot create SYSTEM knowledge documents
            log.warn("User {} attempted to create a SYSTEM document without authorization. Rejecting.", userId);
            throw new AccessDeniedException("Only authorized system accounts can publish SYSTEM documents.");
        }

        User owner = null;
        if (userId != null) {
            owner = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        }

        String docId = (request.documentId() != null && !request.documentId().isBlank())
                ? request.documentId().trim()
                : UUID.randomUUID().toString();

        String normalizedContent = documentChunker.normalizeContent(request.content());
        String checksum = documentChunker.calculateChecksum(normalizedContent);

        // Check for existing document (idempotency check)
        Optional<KnowledgeDocument> existingOpt = (userId != null)
                ? documentRepository.findByDocumentIdAndOwnerId(docId, userId)
                : documentRepository.findByDocumentIdAndOwnerIsNull(docId);

        if (existingOpt.isPresent()) {
            KnowledgeDocument existing = existingOpt.get();
            if (checksum.equals(existing.getChecksum())) {
                log.info("Idempotent ingestion: Document '{}' for owner {} unchanged (checksum: {}). Skipping re-indexing.",
                        docId, userId, checksum);
                return KnowledgeDocumentResponse.from(existing);
            }

            log.info("Re-indexing updated document '{}' for owner {} (old checksum: {}, new checksum: {})",
                    docId, userId, existing.getChecksum(), checksum);

            // Delete old vectors
            deleteVectorChunks(docId, userId);

            // Chunk new content
            List<Document> chunks = documentChunker.chunkDocument(
                    docId,
                    request.title().trim(),
                    normalizedContent,
                    request.documentType(),
                    request.source(),
                    userId,
                    visibility,
                    checksum
            );

            // Index new chunks
            if (!chunks.isEmpty()) {
                try {
                    vectorStore.add(chunks);
                } catch (Exception e) {
                    log.error("Failed to re-index vector chunks for document '{}': {}", docId, e.getMessage());
                    throw new IllegalStateException("Failed to generate embeddings and re-index knowledge document. Please try again.", e);
                }
            }

            existing.setTitle(request.title().trim());
            existing.setContent(normalizedContent);
            existing.setDocumentType(request.documentType());
            existing.setSource(request.source());
            existing.setVisibility(visibility);
            existing.setTags(request.tags());
            existing.setChecksum(checksum);
            existing.setChunkCount(chunks.size());

            KnowledgeDocument updated = documentRepository.save(existing);
            log.info("Successfully re-indexed document '{}' with {} chunks", docId, chunks.size());
            return KnowledgeDocumentResponse.from(updated);
        }

        // New document ingestion
        List<Document> chunks = documentChunker.chunkDocument(
                docId,
                request.title().trim(),
                normalizedContent,
                request.documentType(),
                request.source(),
                userId,
                visibility,
                checksum
        );

        if (!chunks.isEmpty()) {
            try {
                vectorStore.add(chunks);
            } catch (Exception e) {
                log.error("Failed to index vector chunks into vector store for document '{}': {}", docId, e.getMessage());
                throw new IllegalStateException("Failed to generate embeddings and index knowledge document. Please try again.", e);
            }
        }

        KnowledgeDocument newDoc = KnowledgeDocument.builder()
                .documentId(docId)
                .title(request.title().trim())
                .content(normalizedContent)
                .documentType(request.documentType())
                .source(request.source())
                .owner(owner)
                .visibility(visibility)
                .tags(request.tags())
                .checksum(checksum)
                .chunkCount(chunks.size())
                .build();

        KnowledgeDocument saved = documentRepository.save(newDoc);
        log.info("Successfully indexed new knowledge document '{}' (owner: {}, visibility: {}, chunks: {})",
                docId, userId, visibility, chunks.size());
        return KnowledgeDocumentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeQueryResult> searchKnowledge(Long userId, String query, DocumentType documentType, Integer topK) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String sanitizedQuery = query.trim();
        if (sanitizedQuery.length() > 1000) {
            sanitizedQuery = sanitizedQuery.substring(0, 1000);
        }

        int effectiveTopK = (topK != null && topK > 0) ? Math.min(topK, 20) : ragProperties.getTopK();
        double effectiveThreshold = ragProperties.getSimilarityThreshold();

        long startTime = System.currentTimeMillis();

        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            FilterExpressionBuilder.Op authFilter;

            if (userId != null) {
                // Allowed: SYSTEM OR SHARED OR (PRIVATE AND ownerId == userId)
                authFilter = b.or(
                        b.or(b.eq("visibility", KnowledgeVisibility.SYSTEM.name()), b.eq("visibility", KnowledgeVisibility.SHARED.name())),
                        b.and(b.eq("visibility", KnowledgeVisibility.PRIVATE.name()), b.eq("ownerId", userId))
                );
            } else {
                // Unauthenticated or system-only retrieval
                authFilter = b.or(
                        b.eq("visibility", KnowledgeVisibility.SYSTEM.name()),
                        b.eq("visibility", KnowledgeVisibility.SHARED.name())
                );
            }

            FilterExpressionBuilder.Op finalFilter = authFilter;
            if (documentType != null) {
                finalFilter = b.and(authFilter, b.eq("documentType", documentType.name()));
            }

            SearchRequest searchRequest = SearchRequest.builder()
                    .query(sanitizedQuery)
                    .topK(effectiveTopK)
                    .similarityThreshold(effectiveThreshold)
                    .filterExpression(finalFilter.build())
                    .build();

            List<Document> rawResults = vectorStore.similaritySearch(searchRequest);
            long latencyMs = System.currentTimeMillis() - startTime;

            log.info("RAG search query='{}', user={}, documentType={}, found={}, latencyMs={}",
                    query.trim(), userId, documentType, rawResults != null ? rawResults.size() : 0, latencyMs);

            if (rawResults == null || rawResults.isEmpty()) {
                return Collections.emptyList();
            }

            // Convert and deduplicate
            Map<String, KnowledgeQueryResult> resultsByChunkText = new LinkedHashMap<>();
            for (Document doc : rawResults) {
                Map<String, Object> meta = doc.getMetadata();
                String documentId = (String) meta.getOrDefault("documentId", "");
                String title = (String) meta.getOrDefault("title", "");
                String source = (String) meta.getOrDefault("source", "");
                String docTypeStr = (String) meta.getOrDefault("documentType", "OTHER");
                String visibilityStr = (String) meta.getOrDefault("visibility", "SYSTEM");
                Object ownerVal = meta.get("ownerId");
                Long ownerId = ownerVal instanceof Number ? ((Number) ownerVal).longValue() : null;

                DocumentType type;
                try {
                    type = DocumentType.valueOf(docTypeStr);
                } catch (Exception e) {
                    type = DocumentType.OTHER;
                }

                KnowledgeVisibility vis;
                try {
                    vis = KnowledgeVisibility.valueOf(visibilityStr);
                } catch (Exception e) {
                    vis = KnowledgeVisibility.SYSTEM;
                }

                Double score = doc.getScore();
                String text = doc.getText() != null ? doc.getText().trim() : "";

                KnowledgeQueryResult result = new KnowledgeQueryResult(
                        documentId,
                        title,
                        source,
                        type,
                        vis,
                        ownerId,
                        text,
                        score,
                        meta
                );

                // Deduplicate chunks with identical text
                resultsByChunkText.putIfAbsent(text, result);
            }

            return new ArrayList<>(resultsByChunkText.values());

        } catch (Exception e) {
            log.error("RAG search failure for query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<KnowledgeDocumentResponse> getDocument(Long userId, String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return Optional.empty();
        }
        return documentRepository.findAccessibleDocument(documentId.trim(), userId)
                .map(KnowledgeDocumentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeDocumentResponse> listDocuments(Long userId, Pageable pageable) {
        if (userId != null) {
            return documentRepository.findByOwnerId(userId, pageable)
                    .map(KnowledgeDocumentResponse::from);
        }
        return documentRepository.findByVisibility(KnowledgeVisibility.SYSTEM, pageable)
                .map(KnowledgeDocumentResponse::from);
    }

    @Override
    @Transactional
    public void deleteDocument(Long userId, String documentId) {
        if (documentId == null || documentId.isBlank()) {
            return;
        }
        String cleanId = documentId.trim();

        if (userId != null) {
            documentRepository.deleteByDocumentIdAndOwnerId(cleanId, userId);
        } else {
            documentRepository.deleteByDocumentIdAndOwnerIsNull(cleanId);
        }

        deleteVectorChunks(cleanId, userId);
        log.info("Deleted document '{}' and associated vector chunks for owner {}", cleanId, userId);
    }

    private void deleteVectorChunks(String documentId, Long userId) {
        try {
            if (userId != null) {
                jdbcTemplate.update(
                        "DELETE FROM vector_store WHERE metadata->>'documentId' = ? AND (metadata->>'ownerId' = ? OR metadata->>'ownerId' IS NULL)",
                        documentId, String.valueOf(userId)
                );
            } else {
                jdbcTemplate.update(
                        "DELETE FROM vector_store WHERE metadata->>'documentId' = ?",
                        documentId
                );
            }
        } catch (Exception e) {
            log.error("Failed to delete vector chunks for document '{}': {}", documentId, e.getMessage(), e);
        }
    }
}
  