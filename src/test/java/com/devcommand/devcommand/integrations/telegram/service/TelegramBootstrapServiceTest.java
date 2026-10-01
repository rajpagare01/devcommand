package com.devcommand.devcommand.integrations.telegram.service;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramBootstrapToken;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramBootstrapTokenRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TelegramBootstrapServiceTest {

    private ExternalIdentityRepository identityRepository;
    private UserRepository userRepository;
    private TelegramBootstrapTokenRepository tokenRepository;
    private TelegramBootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        identityRepository = mock(ExternalIdentityRepository.class);
        userRepository = mock(UserRepository.class);
        tokenRepository = mock(TelegramBootstrapTokenRepository.class);
        bootstrapService = new TelegramBootstrapService(identityRepository, userRepository, tokenRepository);
    }

    @Test
    void generateTokenForUser_whenUserExists_createsToken() {
        when(userRepository.existsById(1L)).thenReturn(true);
        String token = bootstrapService.generateTokenForUser(1L);

        assertNotNull(token);
        assertFalse(token.contains("-"));
        
        ArgumentCaptor<TelegramBootstrapToken> captor = ArgumentCaptor.forClass(TelegramBootstrapToken.class);
        verify(tokenRepository).save(captor.capture());
        
        TelegramBootstrapToken savedToken = captor.getValue();
        assertEquals(1L, savedToken.getUserId());
        assertTrue(savedToken.getExpiresAt().isAfter(OffsetDateTime.now()));
    }

    @Test
    void generateTokenForUser_whenUserDoesNotExist_throwsException() {
        when(userRepository.existsById(1L)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> bootstrapService.generateTokenForUser(1L));
    }

    @Test
    void linkAccount_whenTokenValid_linksAccount() {
        String plaintextToken = "validtoken123";
        // Need to hash it properly for the mock to match if we use eq()
        // We'll just use any() for simplicity
        
        TelegramBootstrapToken entity = new TelegramBootstrapToken("hash", 1L, OffsetDateTime.now().plusMinutes(5));
        when(tokenRepository.findById(anyString())).thenReturn(Optional.of(entity));
        when(identityRepository.existsByProviderAndExternalId(ExternalIdentityProvider.TELEGRAM, "123456")).thenReturn(false);
        
        when(tokenRepository.deleteByTokenHash(anyString())).thenReturn(1);
        
        User mockUser = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(mockUser, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        String result = bootstrapService.linkAccount(plaintextToken, "123456");

        assertEquals("Account successfully linked! You can now use DevCommand.", result);
        verify(tokenRepository).deleteByTokenHash(anyString());
        verify(identityRepository).save(any());
    }

    @Test
    void linkAccount_whenTokenExpired_returnsError() {
        TelegramBootstrapToken entity = new TelegramBootstrapToken("hash", 1L, OffsetDateTime.now().minusMinutes(5));
        when(tokenRepository.findById(anyString())).thenReturn(Optional.of(entity));
        when(tokenRepository.deleteByTokenHash(anyString())).thenReturn(1);

        String result = bootstrapService.linkAccount("sometoken", "123456");

        assertEquals("Token has expired.", result);
        verify(tokenRepository).deleteByTokenHash(anyString()); // Even expired tokens are consumed
        verify(identityRepository, never()).save(any());
    }

    @Test
    void linkAccount_whenIdentityConflict_returnsError() {
        TelegramBootstrapToken entity = new TelegramBootstrapToken("hash", 1L, OffsetDateTime.now().plusMinutes(5));
        when(tokenRepository.findById(anyString())).thenReturn(Optional.of(entity));
        when(tokenRepository.deleteByTokenHash(anyString())).thenReturn(1);
        when(identityRepository.existsByProviderAndExternalId(ExternalIdentityProvider.TELEGRAM, "123456")).thenReturn(true);

        String result = bootstrapService.linkAccount("sometoken", "123456");

        assertEquals("This Telegram account is already linked to a user.", result);
        verify(tokenRepository).deleteByTokenHash(anyString());
        verify(identityRepository, never()).save(any());
    }

    @Test
    void linkAccount_whenConcurrency_preventsReuse() throws InterruptedException {
        // First call succeeds deleting the row
        when(tokenRepository.findById(anyString()))
            .thenReturn(Optional.of(new TelegramBootstrapToken("hash", 1L, OffsetDateTime.now().plusMinutes(5))));
        when(tokenRepository.deleteByTokenHash(anyString()))
            .thenReturn(1) // First time succeeds
            .thenReturn(0); // Second time returns 0 deleted because first time consumed it

        when(identityRepository.existsByProviderAndExternalId(any(), any())).thenReturn(false);
        User mockUser = new User();
        org.springframework.test.util.ReflectionTestUtils.setField(mockUser, "id", 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        String firstResult = bootstrapService.linkAccount("sometoken", "123");
        String secondResult = bootstrapService.linkAccount("sometoken", "123");

        assertEquals("Account successfully linked! You can now use DevCommand.", firstResult);
        assertEquals("Invalid or expired token.", secondResult);
    }
}
