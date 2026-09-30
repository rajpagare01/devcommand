package com.devcommand.devcommand.integrations.telegram.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "telegram_processed_updates")
public class TelegramProcessedUpdate {

    @Id
    @Column(name = "update_id")
    private Long updateId;

    @CreationTimestamp
    @Column(name = "processed_at", updatable = false, nullable = false)
    private OffsetDateTime processedAt;

    protected TelegramProcessedUpdate() {
    }

    public TelegramProcessedUpdate(Long updateId) {
        this.updateId = updateId;
    }

    public Long getUpdateId() {
        return updateId;
    }

    public OffsetDateTime getProcessedAt() {
        return processedAt;
    }
}
