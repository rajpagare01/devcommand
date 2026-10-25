package com.devcommand.devcommand.integrations.ai.config;

import com.devcommand.devcommand.integrations.gemini.config.GeminiProperties;
import com.google.genai.Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpringAiConfig {

    private static final Logger log = LoggerFactory.getLogger(SpringAiConfig.class);

    @Bean(name = "geminiChatModel")
    @ConditionalOnMissingBean(name = "geminiChatModel")
    public ChatModel geminiChatModel(GeminiProperties geminiProperties) {
        String apiKey = geminiProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "unconfigured-gemini-key";
        }

        Client client = Client.builder()
                .apiKey(apiKey)
                .build();

        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(geminiProperties.getModel())
                .responseMimeType("application/json")
                .build();

        log.info("Initialized Spring AI Google GenAI ChatModel with model: {}", geminiProperties.getModel());

        return GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .defaultOptions(options)
                .build();
    }

    @Bean(name = "groqChatModel")
    @ConditionalOnMissingBean(name = "groqChatModel")
    public ChatModel groqChatModel(GroqProperties groqProperties) {
        String apiKey = groqProperties.getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = "unconfigured-groq-key";
        }

        String rawUrl = groqProperties.getApiUrl();
        String baseUrl = "https://api.groq.com/openai/v1";
        String completionsPath = "/chat/completions";

        if (rawUrl != null && !rawUrl.isBlank()) {
            String trimmed = rawUrl.trim();
            if (trimmed.endsWith("/chat/completions")) {
                baseUrl = trimmed.substring(0, trimmed.length() - "/chat/completions".length());
                completionsPath = "/chat/completions";
            } else if (trimmed.endsWith("/v1")) {
                baseUrl = trimmed;
                completionsPath = "/chat/completions";
            } else {
                baseUrl = trimmed;
            }
        }

        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .completionsPath(completionsPath)
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(groqProperties.getModel())
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                .build();

        log.info("Initialized Spring AI Groq (OpenAI-compatible) ChatModel with baseUrl: {}, model: {}", baseUrl, groqProperties.getModel());

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
