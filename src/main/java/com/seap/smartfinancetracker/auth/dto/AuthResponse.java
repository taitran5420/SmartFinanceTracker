package com.seap.smartfinancetracker.auth.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String token,
        long expiresIn
) {
}
