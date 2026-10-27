package com.devcommand.devcommand.integrations.ai.rag.dto;

import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeSearchRequest(
        @NotBlank(message = "Query cannot be blank")
        @Size(max = 1000, message = "Query cannot exceed 1,000 characters")
        String query,

        DocumentType documentType,

        @Min(value = 1, message = "topK must be at least 1")
        @Max(value = 20, message = "topK cannot exceed 20")
        Integer topK
) {}
