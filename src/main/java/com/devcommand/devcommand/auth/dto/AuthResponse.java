package com.devcommand.devcommand.auth.dto;

import com.devcommand.devcommand.user.dto.UserResponse;

public record AuthResponse(
        String token,
        String tokenType,
        UserResponse user
) {
    public static AuthResponse of(String token, UserResponse user) {
        return new AuthResponse(token, "Bearer", user);
    }
}
