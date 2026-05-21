package com.seap.smartfinancetracker.auth.dto;

import lombok.Builder;

/**
 * Response object containing authentication information after a successful login or registration.
 *
 * @param token the JWT access token used for authenticated requests
 * @param expiresIn the token expiration time in seconds
 */
@Builder
public record AuthResponse(
        String token,
        long expiresIn
) {
}
