package com.devcommand.devcommand.integrations.ai.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic, offline-capable embedding model that generates reproducible 1536-dimensional
 * unit vectors. Preserves lexical overlap similarity without requiring external network calls.
 */
public class DeterministicEmbeddingModel extends AbstractEmbeddingModel {

    private final int dimensions;

    public DeterministicEmbeddingModel(int dimensions) {
        this.dimensions = dimensions;
    }

    public DeterministicEmbeddingModel() {
        this(1536);
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public float[] embed(Document document) {
        return generateVector(document != null ? document.getText() : "");
    }

    @Override
    public float[] embed(String text) {
        return generateVector(text);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> results = new ArrayList<>();
        int idx = 0;
        for (String instruction : request.getInstructions()) {
            results.add(new Embedding(generateVector(instruction), idx++));
        }
        return new EmbeddingResponse(results);
    }

    public float[] generateVector(String text) {
        float[] vector = new float[dimensions];
        if (text == null || text.isBlank()) {
            vector[0] = 1.0f;
            return vector;
        }

        String[] tokens = text.toLowerCase().split("\\W+");
        for (String token : tokens) {
            if (token.isBlank()) continue;
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] hash = md.digest(token.getBytes(StandardCharsets.UTF_8));
                for (int i = 0; i < hash.length; i++) {
                    int slot = Math.abs((token.hashCode() * 31 + i * 47)) % dimensions;
                    vector[slot] += (hash[i] / 128.0f);
                }
            } catch (Exception ignored) {}
        }

        // Normalize to unit vector for cosine distance
        double sumSq = 0.0;
        for (float val : vector) {
            sumSq += val * val;
        }
        if (sumSq > 0.0) {
            float norm = (float) Math.sqrt(sumSq);
            for (int i = 0; i < dimensions; i++) {
                vector[i] /= norm;
            }
        } else {
            vector[0] = 1.0f;
        }

        return vector;
    }
}
