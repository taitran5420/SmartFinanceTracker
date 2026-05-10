package com.seap.smartfinancetracker.auth.service;

import com.seap.smartfinancetracker.auth.dto.AuthResponse;
import com.seap.smartfinancetracker.auth.dto.LoginRequest;
import com.seap.smartfinancetracker.auth.dto.RegisterRequest;

/**
 * Service interface for handling authentication-related operations.
 */
public interface AuthService {

    /**
     * Registers a new user account.
     *
     * @param request the registration request containing user account information
     * @return the authentication response containing token information
     */
    AuthResponse register(RegisterRequest request);

    /**
     * Authenticates a user using the provided credentials.
     *
     * @param request the login request containing user credentials
     * @return the authentication response containing token information
     */
    AuthResponse authenticate(LoginRequest request);
}
