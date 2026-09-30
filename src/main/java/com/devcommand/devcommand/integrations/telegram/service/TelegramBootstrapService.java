package com.devcommand.devcommand.integrations.telegram.service;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.user.entity.User;
import com.devcommand.devcommand.user.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class TelegramBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(TelegramBootstrapService.class);

    private final ExternalIdentityRepository identityRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private String currentBootstrapToken;
    private OffsetDateTime tokenExpiry;

    public TelegramBootstrapService(ExternalIdentityRepository identityRepository,
                                    UserRepository userRepository,
                                    PasswordEncoder passwordEncoder) {
        this.identityRepository = identityRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void init() {
        if (identityRepository.count() == 0) {
            currentBootstrapToken = UUID.randomUUID().toString();
            tokenExpiry = OffsetDateTime.now().plusHours(1);
            log.info("=======================================================================");
            log.info("TELEGRAM ADMIN BOOTSTRAP TOKEN GENERATED");
            log.info("Send the following message to your Telegram bot to link your account:");
            log.info("/bootstrap {}", currentBootstrapToken);
            log.info("This token will expire in 1 hour.");
            log.info("=======================================================================");
        }
    }

    @Transactional
    public boolean bootstrapAdmin(String token, String telegramId) {
        if (currentBootstrapToken == null || !currentBootstrapToken.equals(token)) {
            return false;
        }
        if (OffsetDateTime.now().isAfter(tokenExpiry)) {
            currentBootstrapToken = null;
            return false;
        }

        // Token matches and is not expired.
        // Create an admin user if there are no users, or link to the first user.
        User adminUser = userRepository.findAll().stream().findFirst().orElseGet(() -> {
            User newUser = User.builder()
                    .name("System Admin")
                    .email("admin@localhost")
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .build();
            return userRepository.save(newUser);
        });

        UserExternalIdentity identity = new UserExternalIdentity(adminUser, ExternalIdentityProvider.TELEGRAM, telegramId, true);
        identityRepository.save(identity);

        currentBootstrapToken = null; // consume token
        log.info("Admin bootstrap successful for Telegram ID: {}", telegramId);
        return true;
    }
}
