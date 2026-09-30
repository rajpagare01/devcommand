package com.devcommand.devcommand.integrations.telegram.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "telegram")
public class TelegramProperties {
    private String botToken;
    private Long allowedUserId;

    public String getBotToken() {
        return botToken;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public Long getAllowedUserId() {
        return allowedUserId;
    }

    public void setAllowedUserId(Long allowedUserId) {
        this.allowedUserId = allowedUserId;
    }
}
