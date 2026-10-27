package com.devcommand.devcommand.integrations.ai.controller;

import com.devcommand.devcommand.conversation.service.ConversationContextService;
import com.devcommand.devcommand.integrations.ai.dto.AiChatRequest;
import com.devcommand.devcommand.integrations.ai.dto.AiChatResponse;
import com.devcommand.devcommand.integrations.ai.orchestrator.AiOrchestrator;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorRequest;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorResponse;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Slf4j
public class AiChatController {

    private final AiOrchestrator orchestrator;
    private final ConversationContextService contextService;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        Long userId = principal.getId();
        String convId = request.conversationId();

        if (convId != null && !convId.isBlank()) {
            convId = convId.trim();
            if (contextService.isOwnedByAnotherUser(convId, userId)) {
                log.warn("Access denied: user {} attempted to access conversation {} owned by another user", userId, convId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new AiChatResponse(
                                com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorStatus.ERROR,
                                "Access denied: Conversation belongs to another user.",
                                convId,
                                java.util.List.of(),
                                null,
                                null
                        ));
            }
        } else {
            convId = UUID.randomUUID().toString();
        }

        OrchestratorRequest orchReq = new OrchestratorRequest(
                userId,
                request.message(),
                convId,
                request.conversationContext()
        );

        OrchestratorResponse orchResp = orchestrator.process(orchReq);
        return ResponseEntity.ok(AiChatResponse.from(orchResp));
    }
}
