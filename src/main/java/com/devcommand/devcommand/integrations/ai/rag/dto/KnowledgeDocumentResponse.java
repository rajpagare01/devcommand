package com.devcommand.devcommand.integrations.ai.rag.dto;

import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeDocument;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeVisibility;

import java.time.LocalDateTime;

public record KnowledgeDocumentResponse(
        Long id,
        String documentId,
        String title,
        DocumentType documentType,
        String source,
        Long ownerId,
        KnowledgeVisibility visibility,
        String tags,
        String checksum,
        int chunkCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static KnowledgeDocumentResponse from(KnowledgeDocument doc) {
        return new KnowledgeDocumentResponse(
                doc.getId(),
                doc.getDocumentId(),
                doc.getTitle(),
                doc.getDocumentType(),
                doc.getSource(),
                doc.getOwner() != null ? doc.getOwner().getId() : null,
                doc.getVisibility(),
                doc.getTags(),
                doc.getChecksum(),
                doc.getChunkCount() != null ? doc.getChunkCount() : 0,
                doc.getCreatedAt(),
                doc.getUpdatedAt()
        );
    }
}
