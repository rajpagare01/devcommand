package com.devcommand.devcommand.integrations.telegram.service;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.integrations.telegram.entity.TelegramBootstrapToken;
import com.devcommand.devcommand.integrations.telegram.repository.TelegramBootstrapTokenRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

@Service
public class TelegramBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBootstrapService.class);

    private final ExternalIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final TelegramBootstrapTokenRepository tokenRepository;

    public TelegramBootstrapService(ExternalIdentityRepository identityRepository,
                                    UserRepository userRepository,
                                    TelegramBootstrapTokenRepository tokenRepository) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
    }

    @Transactional
    public String generateTokenForUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User does not exist");
        }

        // Generate a cryptographically secure token
        String plaintextToken = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashToken(plaintextToken);

        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(5);

        TelegramBootstrapToken tokenEntity = new TelegramBootstrapToken(tokenHash, userId, expiresAt);
        tokenRepository.save(tokenEntity);

        return plaintextToken;
    }

    @Transactional
    public String linkAccount(String token, String telegramId) {
        String tokenHash = hashToken(token);
        
        Optional<TelegramBootstrapToken> tokenOpt = tokenRepository.findById(tokenHash);
        if (tokenOpt.isEmpty()) {
            return "Invalid or expired token.";
        }

        int deleted = tokenRepository.deleteByTokenHash(tokenHash); // Consume atomically
        if (deleted == 0) {
            return "Invalid or expired token."; // Another thread consumed it first
        }

        TelegramBootstrapToken tokenEntity = tokenOpt.get();

        if (OffsetDateTime.now().isAfter(tokenEntity.getExpiresAt())) {
            return "Token has expired.";
        }

        if (identityRepository.existsByProviderAndExternalId(ExternalIdentityProvider.TELEGRAM, telegramId)) {
            return "This Telegram account is already linked to a user.";
        }
        
        User user = userRepository.findById(tokenEntity.getUserId()).orElse(null);
        if (user == null) {
            return "Associated user no longer exists.";
        }

        UserExternalIdentity identity = new UserExternalIdentity(user, ExternalIdentityProvider.TELEGRAM, telegramId, true);
        identityRepository.save(identity);

        log.info("Telegram ID {} successfully linked to user ID {}", telegramId, user.getId());
        return "Account successfully linked! You can now use DevCommand.";
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
