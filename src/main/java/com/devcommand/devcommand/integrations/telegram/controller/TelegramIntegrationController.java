package com.devcommand.devcommand.integrations.telegram.controller;

import com.devcommand.devcommand.integrations.telegram.service.TelegramBootstrapService;
import com.devcommand.devcommand.security.UserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/integrations/telegram")
public class TelegramIntegrationController {

    private final TelegramBootstrapService bootstrapService;

    public TelegramIntegrationController(TelegramBootstrapService bootstrapService) {
        this.bootstrapService = bootstrapService;
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<Map<String, String>> generateBootstrapToken(@AuthenticationPrincipal UserPrincipal userPrincipal) {
        String token = bootstrapService.generateTokenForUser(userPrincipal.getId());
        return ResponseEntity.ok(Map.of(
                "message", "Send the following token to your Telegram bot using /bootstrap <token>",
                "token", token
        ));
    }
}
