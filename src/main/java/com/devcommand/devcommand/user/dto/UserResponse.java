package com.devcommand.devcommand.user.dto;

import com.devcommand.devcommand.user.entity.User;

import java.time.LocalDateTime;

/**
 * Safe, outward-facing representation of a User. Never carries the password
 * hash. Used anywhere a user needs to be returned from an API.
 */
public record UserResponse(
        Long id,
        String name,
        String email,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt()
        );
    }
}
