package com.devcommand.devcommand.command.handler;

import com.devcommand.devcommand.command.Command;
import com.devcommand.devcommand.command.CommandResult;
import com.devcommand.devcommand.command.CommandType;
import com.devcommand.devcommand.integrations.ai.rag.model.DocumentType;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeQueryResult;
import com.devcommand.devcommand.integrations.ai.rag.service.RagKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchKnowledgeCommandHandler implements CommandHandler {

    private final RagKnowledgeService ragKnowledgeService;

    @Override
    public boolean supports(CommandType type) {
        return type == CommandType.SEARCH_KNOWLEDGE;
    }

    @Override
    public CommandResult handle(Command command) {
        String query = command.parameters().optionalString("query").orElse("").trim();
        if (query.isBlank()) {
            return CommandResult.failure("Search query is required.");
        }

        DocumentType docType = null;
        String rawDocType = command.parameters().optionalString("documentType").orElse("").trim();
        if (!rawDocType.isBlank()) {
            try {
                docType = DocumentType.valueOf(rawDocType.toUpperCase());
            } catch (Exception ignored) {}
        }

        List<KnowledgeQueryResult> results = ragKnowledgeService.searchKnowledge(command.userId(), query, docType, null);
        if (results.isEmpty()) {
            return CommandResult.success("I don't have enough indexed DevCommand documentation to answer that accurately.");
        }

        StringBuilder sb = new StringBuilder();
        Set<String> sources = new LinkedHashSet<>();
        for (KnowledgeQueryResult res : results) {
            sources.add(res.getAttribution());
            sb.append("\n---\n")
              .append("Source: ").append(res.getAttribution()).append("\n")
              .append(res.content()).append("\n");
        }

        StringBuilder response = new StringBuilder();
        response.append(sb.toString().trim());
        response.append("\n\nSources:\n");
        for (String src : sources) {
            response.append("- ").append(src).append("\n");
        }

        return CommandResult.success(response.toString().trim());
    }
}
