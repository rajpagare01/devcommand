package com.devcommand.devcommand.integrations.ai.rag.service;

import com.devcommand.devcommand.integrations.ai.rag.config.RagProperties;
import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeVisibility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentChunker {

    private final RagProperties ragProperties;

    public String normalizeContent(String content) {
        if (content == null) return "";
        return content.replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    public String calculateChecksum(String normalizedContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizedContent.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public List<Document> chunkDocument(
            String documentId,
            String title,
            String content,
            DocumentType documentType,
            String source,
            Long ownerId,
            KnowledgeVisibility visibility,
            String checksum
    ) {
        String normalized = normalizeContent(content);
        if (normalized.isBlank()) {
            return List.of();
        }

        Map<String, Object> baseMetadata = new HashMap<>();
        baseMetadata.put("documentId", documentId);
        baseMetadata.put("title", title);
        baseMetadata.put("documentType", documentType.name());
        baseMetadata.put("source", source != null ? source : "");
        baseMetadata.put("visibility", visibility.name());
        baseMetadata.put("checksum", checksum);
        if (ownerId != null) {
            baseMetadata.put("ownerId", ownerId);
        }

        List<Document> resultDocuments = new ArrayList<>();

        // If content is small, single chunk is sufficient
        if (normalized.length() <= ragProperties.getChunkSize() * 4) {
            Map<String, Object> metadata = new HashMap<>(baseMetadata);
            metadata.put("chunkIndex", 0);
            resultDocuments.add(new Document(UUID.randomUUID().toString(), normalized, metadata));
            return resultDocuments;
        }

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(ragProperties.getChunkSize())
                .withMinChunkSizeChars(30)
                .build();

        List<Document> rawChunks = splitter.split(List.of(new Document(normalized, baseMetadata)));
        int index = 0;
        for (Document raw : rawChunks) {
            String text = raw.getText() != null ? raw.getText().trim() : "";
            if (text.isBlank()) continue;

            Map<String, Object> metadata = new HashMap<>(baseMetadata);
            metadata.put("chunkIndex", index);
            resultDocuments.add(new Document(UUID.randomUUID().toString(), text, metadata));
            index++;
        }

        if (resultDocuments.isEmpty()) {
            Map<String, Object> metadata = new HashMap<>(baseMetadata);
            metadata.put("chunkIndex", 0);
            resultDocuments.add(new Document(UUID.randomUUID().toString(), normalized, metadata));
        }

        return resultDocuments;
    }
}
