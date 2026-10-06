package com.devcommand.devcommand.dsa.service;

import com.devcommand.devcommand.dsa.integration.ExternalDsaAccount;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountRepository;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountStatus;
import com.devcommand.devcommand.dsa.platform.DsaPlatform;
import com.devcommand.devcommand.dsa.exception.LeetCodeAccountAlreadyConnectedException;
import com.devcommand.devcommand.dsa.exception.LeetCodeAccountNotFoundException;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ExternalDsaAccountService {

    private final ExternalDsaAccountRepository accountRepository;
    private final UserRepository userRepository;
    private final DsaSyncService syncService;

    public ExternalDsaAccount connectLeetCodeAccount(Long userId, String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username cannot be blank");
        }
        
        String cleanUsername = username.trim();
        
        if (accountRepository.existsByUserIdAndPlatform(userId, DsaPlatform.LEETCODE)) {
            throw new LeetCodeAccountAlreadyConnectedException("A LeetCode account is already connected to this user");
        }

        ExternalDsaAccount account = createDisconnectedAccount(userId, cleanUsername);

        try {
            syncService.syncLeetCodeAccount(userId);
            return accountRepository.findById(account.getId()).orElse(account);
        } catch (Exception e) {
            // Rollback creation if validation fails on first connect
            accountRepository.delete(account);
            throw e;
        }
    }
    
    @Transactional
    protected ExternalDsaAccount createDisconnectedAccount(Long userId, String username) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ExternalDsaAccount account = ExternalDsaAccount.builder()
                .user(user)
                .platform(DsaPlatform.LEETCODE)
                .username(username)
                .status(ExternalDsaAccountStatus.DISCONNECTED)
                .build();
                
        return accountRepository.save(account);
    }

    @Transactional(readOnly = true)
    public ExternalDsaAccount getLeetCodeAccount(Long userId) {
        return accountRepository.findByUserIdAndPlatform(userId, DsaPlatform.LEETCODE)
                .orElseThrow(() -> new LeetCodeAccountNotFoundException("LeetCode account not connected"));
    }

    @Transactional
    public void disconnectLeetCodeAccount(Long userId) {
        ExternalDsaAccount account = getLeetCodeAccount(userId);
        accountRepository.delete(account);
    }
}
