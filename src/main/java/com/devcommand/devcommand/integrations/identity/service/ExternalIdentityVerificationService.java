package com.devcommand.devcommand.integrations.identity.service;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.exception.ResourceNotFoundException;
import com.devcommand.devcommand.exception.BadRequestException;
import com.devcommand.devcommand.integrations.identity.config.ExternalIdentityVerificationProperties;
import com.devcommand.devcommand.integrations.identity.entity.ExternalIdentityVerificationChallenge;
import com.devcommand.devcommand.integrations.identity.entity.UserExternalIdentity;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityRepository;
import com.devcommand.devcommand.integrations.identity.repository.ExternalIdentityVerificationChallengeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ExternalIdentityVerificationService {

    private static final Logger log = LoggerFactory.getLogger(ExternalIdentityVerificationService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    private final ExternalIdentityVerificationChallengeRepository challengeRepository;
    private final ExternalIdentityRepository identityRepository;
    private final ExternalIdentityVerificationProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final List<VerificationChallengeSender> senders;

    public ExternalIdentityVerificationService(ExternalIdentityVerificationChallengeRepository challengeRepository,
                                               ExternalIdentityRepository identityRepository,
                                               ExternalIdentityVerificationProperties properties,
                                               PasswordEncoder passwordEncoder,
                                               List<VerificationChallengeSender> senders) {
        this.challengeRepository = challengeRepository;
        this.identityRepository = identityRepository;
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.senders = senders;
    }

    public void startVerification(Long userId, Long identityId) {
        UserExternalIdentity identity = identityRepository.findByIdAndUserId(identityId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity not found"));

        if (identity.isVerified()) {
            log.info("Identity {} is already verified", identityId);
            return;
        }

        String rawCode = generateSecureCode();
        String hashedCode = passwordEncoder.encode(rawCode);
        OffsetDateTime expiresAt = OffsetDateTime.now().plus(properties.getTtl());

        ExternalIdentityVerificationChallenge challenge = new ExternalIdentityVerificationChallenge(
                identity, hashedCode, expiresAt
        );
        challengeRepository.save(challenge);

        VerificationChallengeSender sender = senders.stream()
                .filter(s -> s.supports(identity.getProvider()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No delivery sender for provider: " + identity.getProvider()));

        log.info("Started identity verification for identity ID {}", identityId);
        sender.sendVerificationChallenge(identity.getExternalId(), rawCode);
    }

    public void verify(Long userId, Long identityId, String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new BadRequestException("Invalid or expired verification code.");
        }

        UserExternalIdentity identity = identityRepository.findByIdAndUserId(identityId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Identity not found"));

        if (identity.isVerified()) {
            log.info("Identity {} is already verified", identityId);
            return; // Safe idempotent return
        }

        ExternalIdentityVerificationChallenge challenge = challengeRepository.findLatestActiveChallenge(identityId)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification code."));

        if (challenge.getExpiresAt().isBefore(OffsetDateTime.now())) {
            log.info("Verification challenge {} expired", challenge.getId());
            throw new BadRequestException("Invalid or expired verification code.");
        }

        if (challenge.getAttemptCount() >= properties.getMaxAttempts()) {
            log.info("Verification attempt limit reached for challenge {}", challenge.getId());
            throw new BadRequestException("Verification attempt limit reached. Please request a new code.");
        }

        challenge.setAttemptCount(challenge.getAttemptCount() + 1);

        if (!passwordEncoder.matches(code.trim(), challenge.getChallengeHash())) {
            challengeRepository.save(challenge);
            log.info("Identity verification failed for identity ID {}", identityId);
            throw new BadRequestException("Invalid or expired verification code.");
        }

        // Success
        challenge.setConsumedAt(OffsetDateTime.now());
        challengeRepository.save(challenge);

        identity.setVerified(true);
        identityRepository.save(identity);
        
        log.info("Identity verification succeeded for identity ID {}", identityId);
    }

    private String generateSecureCode() {
        int code = SECURE_RANDOM.nextInt(900000) + 100000; // 6 digit OTP: 100000 to 999999
        return String.valueOf(code);
    }
}
