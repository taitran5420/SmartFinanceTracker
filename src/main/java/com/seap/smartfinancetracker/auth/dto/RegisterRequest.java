package com.seap.smartfinancetracker.auth.dto;

public record RegisterRequest(
        String email,
        String password,
        String fullName) {
}
