package com.devcommand.devcommand.integrations.whatsapp.entity;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "external_webhook_events", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"provider", "external_event_id"})
})
public class ExternalWebhookEvent {

    public enum EventStatus {
        RECEIVED,
        PROCESSING,
        PROCESSED,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ExternalIdentityProvider provider;

    @Column(name = "external_event_id", nullable = false)
    private String externalEventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "processed_at")
    private OffsetDateTime processedAt;

    public ExternalWebhookEvent() {
    }

    public ExternalWebhookEvent(ExternalIdentityProvider provider, String externalEventId, EventStatus status) {
        this.provider = provider;
        this.externalEventId = externalEventId;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public ExternalIdentityProvider getProvider() {
        return provider;
    }

    public String getExternalEventId() {
        return externalEventId;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(OffsetDateTime processedAt) {
        this.processedAt = processedAt;
    }
}
