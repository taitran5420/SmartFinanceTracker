package com.seap.smartfinancetracker.auth.dto;

/**
 * Request object containing user login credentials.
 *
 * @param email the user's email address
 * @param password the user's password
 */
public record LoginRequest(
        String email,
        String password) {
}
