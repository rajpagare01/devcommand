package com.devcommand.devcommand.integrations.ai.rag.config;

import com.devcommand.devcommand.integrations.ai.rag.service.DeterministicEmbeddingModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@Slf4j
public class RagConfig {

    @Value("${spring.ai.openai.api-key:}")
    private String openAiApiKey;

    @Value("${spring.ai.openai.base-url:https://api.openai.com}")
    private String openAiBaseUrl;

    @Value("${spring.profiles.active:}")
    private String activeProfile;

    @Bean
    @Primary
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel embeddingModel(RagProperties properties) {
        String provider = properties.getEmbeddingProvider();
        boolean hasValidOpenAiKey = openAiApiKey != null && !openAiApiKey.isBlank() && !openAiApiKey.contains("dummy");

        if ("openai".equalsIgnoreCase(provider)) {
            if (hasValidOpenAiKey) {
                log.info("Configuring production OpenAiEmbeddingModel with model: text-embedding-3-small, dimensions: {}", properties.getDimensions());
                OpenAiApi api = OpenAiApi.builder()
                        .apiKey(openAiApiKey)
                        .baseUrl(openAiBaseUrl)
                        .build();

                OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                        .model("text-embedding-3-small")
                        .dimensions(properties.getDimensions())
                        .build();

                return new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options);
            }

            if (activeProfile != null && (activeProfile.contains("prod") || activeProfile.contains("production"))) {
                throw new IllegalStateException("Production environment requires OPENAI_API_KEY to be configured for OpenAI embeddings. Refusing to use mock embedding model in production.");
            }
        }

        log.info("Using DeterministicEmbeddingModel (dimensions: {}, provider setting: {}) for reliable offline/test PGVector operations.",
                properties.getDimensions(), provider);
        return new DeterministicEmbeddingModel(properties.getDimensions());
    }

    @Bean
    @ConditionalOnMissingBean(VectorStore.class)
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel, RagProperties properties) {
        log.info("Configuring PgVectorStore with table: {}, dimensions: {}, initializeSchema: false",
                properties.getVectorTableName(), properties.getDimensions());

        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .vectorTableName(properties.getVectorTableName())
                .dimensions(properties.getDimensions())
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(false)
                .vectorTableValidationsEnabled(false)
                .build();
    }
}
