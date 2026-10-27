package com.devcommand.devcommand.integrations.ai.rag.model;

import java.util.Map;

public record KnowledgeChunk(
        String chunkId,
        String documentId,
        int chunkIndex,
        String text,
        Map<String, Object> metadata
) {}
