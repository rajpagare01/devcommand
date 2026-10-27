package com.devcommand.devcommand.integrations.ai.rag.service;

import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentRequest;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentResponse;
import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeQueryResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface RagKnowledgeService {

    KnowledgeDocumentResponse ingestDocument(Long userId, KnowledgeDocumentRequest request);

    List<KnowledgeQueryResult> searchKnowledge(Long userId, String query, DocumentType documentType, Integer topK);

    Optional<KnowledgeDocumentResponse> getDocument(Long userId, String documentId);

    Page<KnowledgeDocumentResponse> listDocuments(Long userId, Pageable pageable);

    void deleteDocument(Long userId, String documentId);
}
