package com.devcommand.devcommand.integrations.identity.service;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import org.springframework.stereotype.Component;

@Component
public class IdentityNormalizer {

    public String normalize(ExternalIdentityProvider provider, String rawId) {
        if (rawId == null) {
            return "";
        }
        
        String id = rawId.trim();
        
        if (provider == ExternalIdentityProvider.WHATSAPP) {
            // Very basic normalization for conceptual purposes:
            // Strip everything except digits and leading '+'
            return id.replaceAll("[^0-9+]", "");
        }
        
        return id;
    }
}
