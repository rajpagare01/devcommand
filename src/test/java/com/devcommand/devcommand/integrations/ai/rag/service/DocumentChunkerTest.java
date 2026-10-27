package com.devcommand.devcommand.integrations.ai.rag.service;

import com.devcommand.devcommand.integrations.ai.rag.config.RagProperties;
import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeVisibility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkerTest {

    private DocumentChunker chunker;
    private RagProperties properties;

    @BeforeEach
    void setUp() {
        properties = new RagProperties();
        properties.setChunkSize(100);
        properties.setChunkOverlap(20);
        chunker = new DocumentChunker(properties);
    }

    @Test
    @DisplayName("Normalization handles CRLF and redundant newlines")
    void normalizeContent_cleansText() {
        String input = "Line 1\r\n\r\n\r\n\r\nLine 2\r\n   ";
        String normalized = chunker.normalizeContent(input);

        assertEquals("Line 1\n\nLine 2", normalized);
    }

    @Test
    @DisplayName("Calculate checksum produces stable SHA-256 hex string")
    void calculateChecksum_isDeterministic() {
        String content = "DevCommand Architecture Documentation";
        String hash1 = chunker.calculateChecksum(content);
        String hash2 = chunker.calculateChecksum(content);

        assertNotNull(hash1);
        assertEquals(64, hash1.length());
        assertEquals(hash1, hash2);

        String diffHash = chunker.calculateChecksum(content + " modified");
        assertNotEquals(hash1, diffHash);
    }

    @Test
    @DisplayName("Small document produces single chunk preserving metadata")
    void chunkDocument_smallContent_singleChunk() {
        String docId = "doc-123";
        String title = "Auth Overview";
        String content = "DevCommand uses stateless JWT authentication.";
        String checksum = chunker.calculateChecksum(content);

        List<Document> chunks = chunker.chunkDocument(
                docId,
                title,
                content,
                DocumentType.ARCHITECTURE,
                "docs/auth.md",
                101L,
                KnowledgeVisibility.PRIVATE,
                checksum
        );

        assertEquals(1, chunks.size());
        Document chunk = chunks.get(0);
        assertEquals(content, chunk.getText());
        assertEquals(docId, chunk.getMetadata().get("documentId"));
        assertEquals(title, chunk.getMetadata().get("title"));
        assertEquals("ARCHITECTURE", chunk.getMetadata().get("documentType"));
        assertEquals("docs/auth.md", chunk.getMetadata().get("source"));
        assertEquals(101L, chunk.getMetadata().get("ownerId"));
        assertEquals("PRIVATE", chunk.getMetadata().get("visibility"));
        assertEquals(checksum, chunk.getMetadata().get("checksum"));
        assertEquals(0, chunk.getMetadata().get("chunkIndex"));
    }

    @Test
    @DisplayName("Large document produces multiple chunks with preserved metadata")
    void chunkDocument_largeContent_splitsAppropriately() {
        properties.setChunkSize(20);
        chunker = new DocumentChunker(properties);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("This is paragraph ").append(i).append(" explaining the deep internals of DevCommand architecture.\n\n");
        }
        String content = sb.toString();
        String checksum = chunker.calculateChecksum(chunker.normalizeContent(content));

        List<Document> chunks = chunker.chunkDocument(
                "large-doc",
                "Deep Architecture",
                content,
                DocumentType.ARCHITECTURE,
                "wiki",
                null,
                KnowledgeVisibility.SYSTEM,
                checksum
        );

        assertTrue(chunks.size() > 1, "Expected multiple chunks for large content");
        for (int i = 0; i < chunks.size(); i++) {
            Document chunk = chunks.get(i);
            assertFalse(chunk.getText().isBlank());
            assertEquals("large-doc", chunk.getMetadata().get("documentId"));
            assertEquals("Deep Architecture", chunk.getMetadata().get("title"));
            assertEquals("SYSTEM", chunk.getMetadata().get("visibility"));
            assertEquals(i, chunk.getMetadata().get("chunkIndex"));
        }
    }
}
