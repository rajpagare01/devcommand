package com.devcommand.devcommand.integrations.identity.dto;

import com.devcommand.devcommand.command.identity.ExternalIdentityProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LinkIdentityRequest(
        @NotNull(message = "Provider is required")
        ExternalIdentityProvider provider,
        
        @NotBlank(message = "External ID is required")
        String externalId
) {}
