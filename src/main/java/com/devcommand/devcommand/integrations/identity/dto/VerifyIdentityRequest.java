package com.devcommand.devcommand.integrations.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record VerifyIdentityRequest(
        @NotBlank(message = "Verification code is required")
        String code
) {
}
