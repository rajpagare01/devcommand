package com.devcommand.devcommand.integrations.identity.dto;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;

import java.time.OffsetDateTime;

public record IdentityResponse(
        Long id,
        ExternalIdentityProvider provider,
        String externalId,
        boolean verified,
        OffsetDateTime createdAt
) {}
