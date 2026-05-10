package com.seap.smartfinancetracker.security.service;

import com.seap.smartfinancetracker.security.model.UserPrincipal;
import io.jsonwebtoken.Claims;

import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for JWT token generation, parsing, and validation.
 */
public interface JwtService {
    /**
     * Extracts the username/email (subject) from the JWT token.
     *
     * @param token the JWT token
     * @return the username/email in the token
     */
    String extractUsername(String token);

    /**
     * Extracts a specific claim from the JWT token.
     *
     * @param token the JWT token
     * @param claimsResolver function to resolve the desired claim
     * @param <T> type of the claim
     * @return extracted claim value
     */
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);

    /**
     * Generates a JWT token for the given user principal.
     *
     * @param userPrincipal the authenticated user
     * @return generated JWT token
     */
    String generateToken(UserPrincipal userPrincipal);

    /**
     * Generates a JWT token with additional claims.
     *
     * @param extraClaims additional claims to include in token
     * @param userPrincipal the authenticated user
     * @return generated JWT token
     */
    String generateToken(Map<String, Object> extraClaims, UserPrincipal userPrincipal);

    /**
     * Validates whether the JWT token is valid for the given user.
     *
     * @param token the JWT token
     * @param userPrincipal the user to validate against
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token, UserPrincipal userPrincipal);

    /**
     * Returns the JWT token expiration time in seconds.
     *
     * @return expiration time in seconds
     */
    long getExpirationTime();
}
