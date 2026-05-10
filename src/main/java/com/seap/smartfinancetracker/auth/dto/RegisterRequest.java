package com.seap.smartfinancetracker.auth.dto;

/**
 * Request object containing information required for user registration.
 *
 * @param email the user's email address
 * @param password the user's password
 * @param fullName the user's full name
 */
public record RegisterRequest(
        String email,
        String password,
        String fullName) {
}
