package com.seap.smartfinancetracker.security.token;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Custom authentication token used for JWT-based authentication in Spring Security.
 * <p>
 * This token is used in two stages of the authentication flow:
 * <ul>
 *     <li>Unauthenticated state: created by the filter with only a raw JWT token</li>
 *     <li>Authenticated state: created by the authentication provider with user details and authorities</li>
 * </ul>
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Getter
    private final String rawToken;

    private final UserDetails principal;

    /**
     * Creates an unauthenticated JWT authentication token containing only the raw JWT string.
     *
     * <p>This constructor is used by the authentication filter before the token is validated.
     * No authorities are granted at this stage, and the authentication is explicitly marked as
     * <b>not authenticated</b>.
     *
     * @param rawToken the raw JWT token extracted from the Authorization header
     */
    public JwtAuthenticationToken(String rawToken) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.rawToken = rawToken;
        this.principal = null;
        setAuthenticated(false);
    }

    /**
     * Creates an authenticated JWT authentication token containing user details and authorities.
     *
     * <p>This constructor is used by the authentication provider after the JWT has been successfully
     * validated. The token is marked as <b>authenticated</b> and contains the resolved user details
     * and granted authorities.
     *
     * @param principal   the authenticated user details extracted from the token
     * @param rawToken    the original JWT token
     * @param authorities the granted authorities associated with the user
     */
    public JwtAuthenticationToken(UserDetails principal, String rawToken, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.rawToken = rawToken;
        this.principal = principal;
        setAuthenticated(true);
    }

    @Override
    public @Nullable Object getCredentials() {
        return rawToken;
    }

    @Override
    public @Nullable Object getPrincipal() {
        return principal;
    }
}
