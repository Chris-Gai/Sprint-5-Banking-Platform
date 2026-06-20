package com.example.authservice.dto;

public record AuthResponse(
        String token,
        String tokenType,
        long expiresInMs,
        Long userId,
        String username
) {}
