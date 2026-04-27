package com.seap.smartfinancetracker.auth.dto;

public record LoginRequest(
        String email,
        String password) {
}
