package com.seap.smartfinancetracker.auth.controller;

import com.seap.smartfinancetracker.auth.dto.AuthResponse;
import com.seap.smartfinancetracker.auth.dto.LoginRequest;
import com.seap.smartfinancetracker.auth.dto.RegisterRequest;
import com.seap.smartfinancetracker.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller responsible for handling user authentication operations.
 * <p>
 * Provides public endpoints for user registration and login. These endpoints
 * process user credentials and issue authentication tokens
 * required for accessing secured API resources within the application.
 * </p>
 */
@RequestMapping("/auth")
@RestController
@AllArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Registers a new user account and returns the initial authentication tokens.
     *
     * @param registerRequest the payload containing the new user's registration details
     * @return a {@link ResponseEntity} containing the {@link AuthResponse}
     *         with HTTP status 200 (OK)
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest registerRequest) {
        AuthResponse authResponse = authService.register(registerRequest);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Authenticates an existing user and returns authentication tokens.
     *
     * @param loginRequest the payload containing the user's login credentials
     * @return a {@link ResponseEntity} containing the {@link AuthResponse}
     *         with HTTP status 200 (OK)
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.authenticate(loginRequest));
    }
}
