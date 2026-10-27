package com.devcommand.devcommand.integrations.ai.rag.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "ai.rag")
@Getter
@Setter
public class RagProperties {

    private boolean enabled = true;
    private int topK = 4;
    private double similarityThreshold = 0.65;
    private int chunkSize = 500;
    private int chunkOverlap = 50;
    private int dimensions = 1536;
    private String embeddingProvider = "openai";
    private String vectorTableName = "vector_store";
}
