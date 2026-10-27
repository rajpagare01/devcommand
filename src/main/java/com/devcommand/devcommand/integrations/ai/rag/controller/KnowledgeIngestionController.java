package com.devcommand.devcommand.integrations.ai.rag.controller;

import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentRequest;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeDocumentResponse;
import com.devcommand.devcommand.integrations.ai.rag.dto.KnowledgeSearchRequest;
import com.devcommand.devcommand.integrations.ai.rag.model.KnowledgeQueryResult;
import com.devcommand.devcommand.integrations.ai.rag.service.RagKnowledgeService;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai/knowledge")
@RequiredArgsConstructor
@Slf4j
public class KnowledgeIngestionController {

    private final RagKnowledgeService ragKnowledgeService;

    @PostMapping("/documents")
    public ResponseEntity<KnowledgeDocumentResponse> ingestDocument(
            @Valid @RequestBody KnowledgeDocumentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        log.info("REST request to ingest knowledge document '{}' from user {}", request.title(), userId);
        KnowledgeDocumentResponse response = ragKnowledgeService.ingestDocument(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/documents")
    public ResponseEntity<Page<KnowledgeDocumentResponse>> listDocuments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Long userId = principal != null ? principal.getId() : null;
        return ResponseEntity.ok(ragKnowledgeService.listDocuments(userId, pageable));
    }

    @GetMapping("/documents/{documentId}")
    public ResponseEntity<KnowledgeDocumentResponse> getDocument(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        return ragKnowledgeService.getDocument(userId, documentId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/documents/{documentId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable String documentId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        ragKnowledgeService.deleteDocument(userId, documentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/search")
    public ResponseEntity<List<KnowledgeQueryResult>> search(
            @Valid @RequestBody KnowledgeSearchRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal != null ? principal.getId() : null;
        List<KnowledgeQueryResult> results = ragKnowledgeService.searchKnowledge(
                userId,
                request.query(),
                request.documentType(),
                request.topK()
        );
        return ResponseEntity.ok(results);
    }
}
