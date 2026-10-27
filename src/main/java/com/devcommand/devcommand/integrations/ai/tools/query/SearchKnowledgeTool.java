package com.devcommand.devcommand.integrations.ai.tools.query;

import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeQueryResult;
import com.devcommand.devcommand.integrations.ai.rag.service.RagKnowledgeService;
import com.devcommand.devcommand.integrations.ai.tools.AiTool;
import com.devcommand.devcommand.integrations.ai.tools.ToolExecutionResult;
import com.devcommand.devcommand.integrations.ai.tools.ToolParameter;
import com.devcommand.devcommand.integrations.ai.tools.ToolSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchKnowledgeTool implements AiTool {

    private final RagKnowledgeService ragKnowledgeService;

    @Override
    public String getName() {
        return "search_knowledge";
    }

    @Override
    public String getDescription() {
        return "Search indexed DevCommand project documentation, architecture guides, technical decisions, and notes using vector semantic retrieval.";
    }

    @Override
    public ToolSchema getSchema() {
        return ToolSchema.of(
                "Searches unstructured project documentation and knowledge using semantic retrieval",
                ToolParameter.required("query", "string", "Keywords or natural language question to look up in documentation"),
                ToolParameter.optional("documentType", "string", "Optional document category (e.g. ARCHITECTURE, TECHNICAL_DECISION, INTEGRATION_GUIDE, API_DOCUMENTATION, NOTE)")
        );
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> parameters, Long userId) {
        if (parameters == null || !parameters.containsKey("query")) {
            return ToolExecutionResult.failure("Search query is required.");
        }

        String query = parameters.get("query") != null ? parameters.get("query").toString().trim() : "";
        if (query.isBlank()) {
            return ToolExecutionResult.failure("Search query cannot be blank.");
        }

        DocumentType docType = null;
        if (parameters.containsKey("documentType") && parameters.get("documentType") != null) {
            String rawType = parameters.get("documentType").toString().trim().toUpperCase();
            try {
                docType = DocumentType.valueOf(rawType);
            } catch (IllegalArgumentException ignored) {
                // Ignore invalid docType and search across all
            }
        }

        List<KnowledgeQueryResult> results;
        try {
            results = ragKnowledgeService.searchKnowledge(userId, query, docType, null);
        } catch (Exception e) {
            log.error("Knowledge search failed for user {}: {}", userId, e.getMessage(), e);
            return ToolExecutionResult.failure("Knowledge retrieval temporarily unavailable.");
        }

        if (results.isEmpty()) {
            return ToolExecutionResult.success(
                    "I don't have enough indexed DevCommand documentation to answer that accurately.",
                    List.of()
            );
        }

        StringBuilder sb = new StringBuilder();
        Set<String> sources = new LinkedHashSet<>();

        for (KnowledgeQueryResult res : results) {
            String attribution = res.getAttribution();
            if (attribution != null && !attribution.isBlank()) {
                sources.add(attribution);
            }
            sb.append("\n---\n");
            sb.append("Source: ").append(attribution).append("\n");
            sb.append("Content:\n").append(res.content()).append("\n");
        }

        StringBuilder response = new StringBuilder();
        response.append("Found ").append(results.size()).append(" relevant knowledge reference(s):\n");
        response.append(sb);
        response.append("\nSources:\n");
        for (String src : sources) {
            response.append("- ").append(src).append("\n");
        }

        return ToolExecutionResult.success(response.toString().trim(), results);
    }
}
