package com.devcommand.devcommand.dsa.service;

import com.devcommand.devcommand.dsa.integration.ExternalDsaAccount;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountRepository;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountStatus;
import com.devcommand.devcommand.dsa.integration.LeetCodeStatsRepository;
import com.devcommand.devcommand.dsa.platform.DsaPlatform;
import com.devcommand.devcommand.dsa.platform.DsaPlatformAdapter;
import com.devcommand.devcommand.dsa.platform.leetcode.LeetCodeProfile;
import com.devcommand.devcommand.dsa.exception.LeetCodeIntegrationException;
import com.devcommand.devcommand.metrics.DevCommandMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DsaSyncServiceTest {

    @Mock
    private ExternalDsaAccountRepository accountRepository;

    @Mock
    private LeetCodeStatsRepository statsRepository;

    @Mock
    private DsaPlatformAdapter adapter;
    
    @Mock
    private DevCommandMetrics metrics;

    private DsaSyncService service;

    @BeforeEach
    void setUp() {
        when(adapter.getPlatform()).thenReturn(DsaPlatform.LEETCODE);
        service = new DsaSyncService(accountRepository, statsRepository, List.of(adapter), metrics);
    }

    @Test
    void syncLeetCodeAccount_Success() {
        Long userId = 1L;
        ExternalDsaAccount account = new ExternalDsaAccount();
        account.setId(10L);
        account.setUsername("testuser");

        LeetCodeProfile profile = LeetCodeProfile.builder()
                .username("testuser")
                .profileUrl("https://leetcode.com/u/testuser/")
                .totalSolved(100)
                .build();

        when(accountRepository.findByUserIdAndPlatform(userId, DsaPlatform.LEETCODE)).thenReturn(Optional.of(account));
        when(adapter.fetchProfile("testuser")).thenReturn(profile);
        when(statsRepository.findByExternalDsaAccountId(10L)).thenReturn(Optional.empty());

        service.syncLeetCodeAccount(userId);

        assertEquals(ExternalDsaAccountStatus.CONNECTED, account.getStatus());
        verify(statsRepository).save(any());
        verify(accountRepository).save(account);
    }

    @Test
    void syncLeetCodeAccount_Failure() {
        Long userId = 1L;
        ExternalDsaAccount account = new ExternalDsaAccount();
        account.setId(10L);
        account.setUsername("testuser");

        when(accountRepository.findByUserIdAndPlatform(userId, DsaPlatform.LEETCODE)).thenReturn(Optional.of(account));
        when(adapter.fetchProfile("testuser")).thenThrow(new RuntimeException("API error"));

        assertThrows(LeetCodeIntegrationException.class, () -> service.syncLeetCodeAccount(userId));

        assertEquals(ExternalDsaAccountStatus.SYNC_FAILED, account.getStatus());
        verify(accountRepository).save(account);
        verify(statsRepository, never()).save(any());
    }
}
