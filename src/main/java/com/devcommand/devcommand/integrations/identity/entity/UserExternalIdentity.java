package com.devcommand.devcommand.integrations.identity.entity;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import com.devcommand.devcommand.user.entity.User;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "external_identities", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "external_id"})
})
public class UserExternalIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExternalIdentityProvider provider;

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @Column(nullable = false)
    private boolean verified;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public UserExternalIdentity() {
    }

    public UserExternalIdentity(User user, ExternalIdentityProvider provider, String externalId, boolean verified) {
        this.user = user;
        this.provider = provider;
        this.externalId = externalId;
        this.verified = verified;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public ExternalIdentityProvider getProvider() {
        return provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
