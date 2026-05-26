package com.seap.smartfinancetracker.security.provider;

import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.security.service.JwtService;
import com.seap.smartfinancetracker.security.token.JwtAuthenticationToken;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

/**
 * Custom Authentication provider responsible for validating JWT tokens and building an authenticated user context.
 *
 * <p>This provider implements a fully <b>stateless</b> authentication mechanism by performing the following steps:
 * <ul>
 * <li>Validates that the incoming authentication request is a {@link JwtAuthenticationToken}.</li>
 * <li>Cryptographically validates the JWT signature and expiration via {@link JwtService}.</li>
 * <li>Extracts user identity and role claims directly from the token payload to construct a {@link UserPrincipal}
 * (bypassing redundant database lookups to optimize performance).</li>
 * <li>Returns a fully authenticated {@link JwtAuthenticationToken} containing the user's authorities.</li>
 * </ul>
 *
 * <p>If the token is mathematically invalid, expired, or malformed,
 * a {@link BadCredentialsException} is thrown to safely halt the authentication process.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private static final String UNSUPPORTED_AUTHENTICATION_TYPE_MESSAGE = "Unsupported authentication type";
    private static final String INVALID_TOKEN_MESSAGE = "Invalid or expired JWT token";
    private static final String INVALID_TOKEN_PAYLOAD_MESSAGE = "Invalid JWT payload structure";

    private final JwtService jwtService;

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
            throw new AuthenticationServiceException(UNSUPPORTED_AUTHENTICATION_TYPE_MESSAGE);
        }

        String rawToken = jwtAuthenticationToken.getRawToken();

        try {
            if (!jwtService.validateToken(rawToken)) {
                throw new BadCredentialsException(INVALID_TOKEN_MESSAGE);
            }

            UserPrincipal userPrincipal = jwtService.extractUserPrincipal(rawToken);

            if (userPrincipal == null || StringUtils.isBlank(userPrincipal.getUsername())) {
                throw new BadCredentialsException(INVALID_TOKEN_PAYLOAD_MESSAGE);
            }

            return new JwtAuthenticationToken(userPrincipal, rawToken, userPrincipal.getAuthorities());

        } catch (UsernameNotFoundException | JwtException ex) {
            log.error("JWT Parsing error: {}", ex.getMessage());
            throw new BadCredentialsException(INVALID_TOKEN_MESSAGE);
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
