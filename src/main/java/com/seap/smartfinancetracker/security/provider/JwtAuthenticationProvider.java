package com.seap.smartfinancetracker.security.provider;

import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.security.token.JwtAuthenticationToken;
import io.jsonwebtoken.JwtException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Custom Authentication provider responsible for validating JWT tokens and building authenticated user context.
 *
 * <p>This provider performs the following steps:
 * <ul>
 *     <li>Validates that the authentication request is a {@link JwtAuthenticationToken}</li>
 *     <li>Extracts the JWT token and parses the username (email)</li>
 *     <li>Loads user details from the database</li>
 *     <li>Validates the token against user details</li>
 *     <li>Returns an authenticated {@link JwtAuthenticationToken} if valid</li>
 * </ul>
 *
 * <p>If the token is invalid, expired, or the user cannot be found,
 * a {@link BadCredentialsException} is thrown.
 */
@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationProvider(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Authenticates the provided JWT authentication request.
     *
     * @param authentication the authentication request containing a JWT token
     * @return an authenticated {@link JwtAuthenticationToken} if the token is valid
     * @throws AuthenticationException if authentication fails due to invalid token or user
     */
    @Override
    public @Nullable Authentication authenticate(@NonNull Authentication authentication) throws AuthenticationException {

        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)) {
            throw new AuthenticationServiceException("Unsupported authentication type");
        }

        String rawToken = jwtAuthenticationToken.getRawToken();

        try {
            String email = jwtService.extractUsername(rawToken);

            if (email == null || email.isEmpty()) {
                throw new BadCredentialsException("Invalid JWT: No subject found");
            }

            UserPrincipal userPrincipal = (UserPrincipal) userDetailsService.loadUserByUsername(email);

            if (!jwtService.validateToken(rawToken, userPrincipal)) {
                throw new BadCredentialsException("Invalid or expired JWT token");
            }

            return new JwtAuthenticationToken(userPrincipal, rawToken, userPrincipal.getAuthorities());

        } catch (UsernameNotFoundException | JwtException ex) {
            throw new BadCredentialsException("Invalid JWT token");
        }
    }

    /**
     * Indicates whether this provider supports the given authentication type.
     *
     * @param authentication the authentication class
     * @return true if this provider supports {@link JwtAuthenticationToken}
     */
    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
