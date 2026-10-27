package com.devcommand.devcommand.integrations.ai.rag.model;

import java.util.Map;

public record KnowledgeQueryResult(
        String documentId,
        String title,
        String source,
        DocumentType documentType,
        KnowledgeVisibility visibility,
        Long ownerId,
        String content,
        Double similarityScore,
        Map<String, Object> metadata
) {
    public String getAttribution() {
        if (title != null && !title.isBlank()) {
            return title + (source != null && !source.isBlank() ? " (" + source + ")" : "");
        }
        return source != null && !source.isBlank() ? source : documentId;
    }
}
