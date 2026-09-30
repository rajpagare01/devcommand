package com.devcommand.devcommand.command.identity;

public record ExternalIdentity(
        ExternalIdentityProvider provider,
        String externalId
) {
    public ExternalIdentity {
        if (provider == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("External ID cannot be blank");
        }
    }
}
