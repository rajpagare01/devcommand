package com.devcommand.devcommand.integrations.ai.rag.dto;

import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentRequest(
        @Size(max = 100)
        String documentId,

        @NotBlank(message = "Title is required")
        @Size(max = 255)
        String title,

        @NotBlank(message = "Content is required")
        @Size(max = 100000, message = "Content cannot exceed 100,000 characters")
        String content,

        @NotNull(message = "Document type is required")
        DocumentType documentType,

        @Size(max = 255)
        String source,

        KnowledgeVisibility visibility,

        @Size(max = 255)
        String tags
) {}
