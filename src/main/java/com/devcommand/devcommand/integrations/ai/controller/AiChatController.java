package com.devcommand.devcommand.integrations.ai.controller;

import com.devcommand.devcommand.integrations.ai.dto.AiChatRequest;
import com.devcommand.devcommand.integrations.ai.dto.AiChatResponse;
import com.devcommand.devcommand.integrations.ai.orchestrator.AiOrchestrator;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorRequest;
import com.devcommand.devcommand.integrations.ai.orchestrator.OrchestratorResponse;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiOrchestrator orchestrator;

    @PostMapping("/chat")
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrchestratorRequest orchReq = new OrchestratorRequest(
                principal.getId(),
                request.message(),
                request.conversationContext()
        );
        OrchestratorResponse orchResp = orchestrator.process(orchReq);
        return ResponseEntity.ok(AiChatResponse.from(orchResp));
    }
}
