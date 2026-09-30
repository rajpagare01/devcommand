package com.devcommand.devcommand.integrations.identity.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "external_identity_verification_challenges")
public class ExternalIdentityVerificationChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "external_identity_id", nullable = false)
    private UserExternalIdentity externalIdentity;

    @Column(name = "challenge_hash", nullable = false)
    private String challengeHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "consumed_at")
    private OffsetDateTime consumedAt;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    @Version
    private Long version;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public ExternalIdentityVerificationChallenge() {
    }

    public ExternalIdentityVerificationChallenge(UserExternalIdentity externalIdentity, String challengeHash, OffsetDateTime expiresAt) {
        this.externalIdentity = externalIdentity;
        this.challengeHash = challengeHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public UserExternalIdentity getExternalIdentity() {
        return externalIdentity;
    }

    public String getChallengeHash() {
        return challengeHash;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public OffsetDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(OffsetDateTime consumedAt) {
        this.consumedAt = consumedAt;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
