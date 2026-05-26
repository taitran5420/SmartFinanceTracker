package com.seap.smartfinancetracker.security.service;

import com.seap.smartfinancetracker.security.model.UserPrincipal;

import java.util.Map;

/**
 * Service responsible for JWT token generation, parsing, and validation.
 */
public interface JwtService {

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
     * Validates whether the JWT token is valid
     *
     * @param token the JWT token
     * @return true if token is valid, false otherwise
     */
    boolean validateToken(String token);

    /**
     * Extracts and constructs a {@link UserPrincipal} from a given JSON Web Token (JWT).
     * <p>
     * This method is a core component of the stateless authentication flow. It is responsible
     * for parsing the provided JWT, cryptographically validating its signature, and mapping
     * the payload claims (such as user ID, email, and roles) into a Spring Security compliant
     * {@link UserPrincipal} object. This resulting principal is subsequently used to populate
     * the application's {@code SecurityContext}.
     * </p>
     *
     * @param token the raw JWT string representing the user's session (typically extracted
     * from the HTTP Authorization header with the "Bearer " prefix removed)
     * @return a fully populated {@link UserPrincipal} derived from the token's payload
     * @throws io.jsonwebtoken.JwtException (or equivalent runtime exception) if the token
     * is mathematically invalid, expired, or tampered with
     */
    UserPrincipal extractUserPrincipal(String token);

    /**
     * Returns the JWT token expiration time in seconds.
     *
     * @return expiration time in seconds
     */
    long getExpirationTime();
}
