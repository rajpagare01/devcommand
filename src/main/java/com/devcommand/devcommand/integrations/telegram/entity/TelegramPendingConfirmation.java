package com.devcommand.devcommand.integrations.telegram.entity;

import com.devcommand.devcommand.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "telegram_pending_confirmations")
public class TelegramPendingConfirmation {

    @Id
    @Column(name = "token", length = 36, nullable = false)
    private String token;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "chat_id", nullable = false)
    private Long chatId;

    @Column(name = "action_type", length = 50, nullable = false)
    private String actionType;

    @Column(name = "task_id", nullable = false)
    private Long taskId;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    protected TelegramPendingConfirmation() {
    }

    public TelegramPendingConfirmation(String token, User user, Long chatId, String actionType, Long taskId, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.chatId = chatId;
        this.actionType = actionType;
        this.taskId = taskId;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public User getUser() {
        return user;
    }

    public Long getChatId() {
        return chatId;
    }

    public String getActionType() {
        return actionType;
    }

    public Long getTaskId() {
        return taskId;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
