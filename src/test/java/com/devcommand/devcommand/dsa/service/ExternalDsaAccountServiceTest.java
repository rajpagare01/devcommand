package com.devcommand.devcommand.dsa.service;

import com.devcommand.devcommand.dsa.exception.LeetCodeAccountAlreadyConnectedException;
import com.devcommand.devcommand.dsa.exception.LeetCodeAccountNotFoundException;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccount;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountRepository;
import com.devcommand.devcommand.dsa.integration.ExternalDsaAccountStatus;
import com.devcommand.devcommand.dsa.platform.DsaPlatform;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalDsaAccountServiceTest {

    @Mock
    private ExternalDsaAccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DsaSyncService syncService;

    @InjectMocks
    private ExternalDsaAccountService service;

    @Test
    void connectLeetCodeAccount_Success() {
        Long userId = 1L;
        String username = "testuser";
        User user = new User();
        user.setId(userId);
        
        ExternalDsaAccount savedAccount = new ExternalDsaAccount();
        savedAccount.setId(10L);
        savedAccount.setUser(user);
        savedAccount.setUsername(username);
        savedAccount.setStatus(ExternalDsaAccountStatus.DISCONNECTED);

        when(accountRepository.existsByUserIdAndPlatform(userId, DsaPlatform.LEETCODE)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(ExternalDsaAccount.class))).thenReturn(savedAccount);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(savedAccount));

        ExternalDsaAccount result = service.connectLeetCodeAccount(userId, username);

        assertNotNull(result);
        assertEquals(username, result.getUsername());
        verify(syncService).syncLeetCodeAccount(userId);
    }

    @Test
    void connectLeetCodeAccount_AlreadyExists() {
        when(accountRepository.existsByUserIdAndPlatform(1L, DsaPlatform.LEETCODE)).thenReturn(true);
        assertThrows(LeetCodeAccountAlreadyConnectedException.class, () -> service.connectLeetCodeAccount(1L, "test"));
    }

    @Test
    void connectLeetCodeAccount_SyncFails_ThrowsExceptionAndDeletes() {
        Long userId = 1L;
        String username = "testuser";
        User user = new User();
        user.setId(userId);
        
        ExternalDsaAccount savedAccount = new ExternalDsaAccount();
        savedAccount.setId(10L);

        when(accountRepository.existsByUserIdAndPlatform(userId, DsaPlatform.LEETCODE)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRepository.save(any(ExternalDsaAccount.class))).thenReturn(savedAccount);
        doThrow(new RuntimeException("Sync failed")).when(syncService).syncLeetCodeAccount(userId);

        assertThrows(RuntimeException.class, () -> service.connectLeetCodeAccount(userId, username));
        verify(accountRepository).delete(savedAccount);
    }
}
