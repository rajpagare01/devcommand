package com.devcommand.devcommand.dsa.service;

import com.devcommand.devcommand.dsa.integration.ExternalDsaAccount;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountRepository;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountStatus;
import com.devcommand.devcommand.dsa.integration.LeetCodeStats;
import com.devcommand.devcommand.dsa.integration.LeetCodeStatsRepository;
import com.devcommand.devcommand.dsa.platform.DsaPlatform;
import com.devcommand.devcommand.dsa.platform.DsaPlatformAdapter;
import com.devcommand.devcommand.dsa.platform.PlatformProfile;
import com.devcommand.devcommand.dsa.exception.LeetCodeAccountNotFoundException;
import com.devcommand.devcommand.dsa.exception.LeetCodeIntegrationException;
import com.devcommand.devcommand.dsa.exception.LeetCodeProfileNotFoundException;
import com.devcommand.devcommand.metrics.DevCommandMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DsaSyncService {

    private final ExternalDsaAccountRepository accountRepository;
    private final LeetCodeStatsRepository statsRepository;
    private final List<DsaPlatformAdapter> adapters;
    private final DevCommandMetrics metrics;

    public void syncLeetCodeAccount(Long userId) {
        log.info("LeetCode sync started for user {}", userId);
        metrics.recordDsaSyncAttempt(DsaPlatform.LEETCODE.name());
        
        ExternalDsaAccount account = accountRepository.findByUserIdAndPlatform(userId, DsaPlatform.LEETCODE)
                .orElseThrow(() -> new LeetCodeAccountNotFoundException("LeetCode account not found"));

        DsaPlatformAdapter adapter = adapters.stream()
                .filter(a -> a.getPlatform() == DsaPlatform.LEETCODE)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("LeetCode adapter not found"));

        try {
            PlatformProfile profile = adapter.fetchProfile(account.getUsername());
            
            if (profile == null) {
                throw new LeetCodeProfileNotFoundException("LeetCode profile not found or private");
            }
            
            saveSuccess(account, profile);
            metrics.recordDsaSyncSuccess(DsaPlatform.LEETCODE.name());
            log.info("LeetCode sync completed for user {}", userId);
        } catch (Exception e) {
            log.warn("LeetCode sync failed for user {}: {}", userId, e.getMessage());
            saveFailure(account);
            metrics.recordDsaSyncFailure(DsaPlatform.LEETCODE.name());
            throw new LeetCodeIntegrationException("Failed to sync LeetCode profile: " + e.getMessage(), e);
        }
    }
    
    @Transactional
    protected void saveSuccess(ExternalDsaAccount account, PlatformProfile profile) {
        account.setProfileUrl(profile.getProfileUrl());
        account.setStatus(ExternalDsaAccountStatus.CONNECTED);
        account.setLastSyncedAt(Instant.now());
        
        LeetCodeStats stats = statsRepository.findByExternalDsaAccountId(account.getId())
                .orElse(LeetCodeStats.builder().externalDsaAccount(account).build());
                
        stats.setTotalSolved(profile.getTotalSolved());
        stats.setEasySolved(profile.getEasySolved());
        stats.setMediumSolved(profile.getMediumSolved());
        stats.setHardSolved(profile.getHardSolved());
        stats.setRanking(profile.getRanking());
        stats.setLastUpdatedAt(Instant.now());
        
        statsRepository.save(stats);
        accountRepository.save(account);
    }
    
    @Transactional
    protected void saveFailure(ExternalDsaAccount account) {
        account.setStatus(ExternalDsaAccountStatus.SYNC_FAILED);
        accountRepository.save(account);
    }
}
