package com.devcommand.devcommand.dsa.controller;

import com.devcommand.devcommand.dsa.dto.ConnectLeetCodeRequest;
import com.devcommand.devcommand.dsa.dto.LeetCodeIntegrationResponse;
import com.devcommand.devcommand.dsa.dto.LeetCodeStatsResponse;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccount;
import com.devcommand.devcommand.dsa.integration.LeetCodeStats;
import com.devcommand.devcommand.dsa.integration.LeetCodeStatsRepository;
import com.devcommand.devcommand.dsa.service.DsaSyncService;
import com.devcommand.devcommand.dsa.service.ExternalDsaAccountService;
import com.devcommand.devcommand.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/integrations/dsa/leetcode")
@RequiredArgsConstructor
public class LeetCodeIntegrationController {

    private final ExternalDsaAccountService accountService;
    private final DsaSyncService syncService;
    private final LeetCodeStatsRepository statsRepository;

    @PostMapping
    public ResponseEntity<LeetCodeIntegrationResponse> connect(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ConnectLeetCodeRequest request) {
            
        ExternalDsaAccount account = accountService.connectLeetCodeAccount(principal.getId(), request.getUsername());
        return ResponseEntity.ok(mapToResponse(account));
    }

    @GetMapping
    public ResponseEntity<LeetCodeIntegrationResponse> getAccount(
            @AuthenticationPrincipal UserPrincipal principal) {
            
        ExternalDsaAccount account = accountService.getLeetCodeAccount(principal.getId());
        return ResponseEntity.ok(mapToResponse(account));
    }

    @PostMapping("/sync")
    public ResponseEntity<LeetCodeIntegrationResponse> sync(
            @AuthenticationPrincipal UserPrincipal principal) {
            
        syncService.syncLeetCodeAccount(principal.getId());
        ExternalDsaAccount account = accountService.getLeetCodeAccount(principal.getId());
        return ResponseEntity.ok(mapToResponse(account));
    }

    @DeleteMapping
    public ResponseEntity<Void> disconnect(
            @AuthenticationPrincipal UserPrincipal principal) {
            
        accountService.disconnectLeetCodeAccount(principal.getId());
        return ResponseEntity.noContent().build();
    }
    
    private LeetCodeIntegrationResponse mapToResponse(ExternalDsaAccount account) {
        LeetCodeStatsResponse statsResponse = null;
        Integer totalSolved = null;
        Integer easy = null;
        Integer medium = null;
        Integer hard = null;
        Integer ranking = null;
        
        LeetCodeStats stats = statsRepository.findByExternalDsaAccountId(account.getId()).orElse(null);
        if (stats != null) {
            totalSolved = stats.getTotalSolved();
            easy = stats.getEasySolved();
            medium = stats.getMediumSolved();
            hard = stats.getHardSolved();
            ranking = stats.getRanking();

            statsResponse = LeetCodeStatsResponse.builder()
                    .totalSolved(totalSolved)
                    .easySolved(easy)
                    .mediumSolved(medium)
                    .hardSolved(hard)
                    .ranking(ranking)
                    .lastUpdatedAt(stats.getLastUpdatedAt())
                    .build();
        }
        
        return LeetCodeIntegrationResponse.builder()
                .id(account.getId())
                .username(account.getUsername())
                .profileUrl(account.getProfileUrl())
                .status(account.getStatus())
                .lastSyncedAt(account.getLastSyncedAt())
                .totalSolved(totalSolved)
                .easy(easy)
                .medium(medium)
                .hard(hard)
                .easySolved(easy)
                .mediumSolved(medium)
                .hardSolved(hard)
                .ranking(ranking)
                .stats(statsResponse)
                .build();
    }
}
