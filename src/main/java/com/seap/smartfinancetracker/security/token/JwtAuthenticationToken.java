package com.seap.smartfinancetracker.security.token;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    @Getter
    private final String rawToken;

    private final UserDetails principal;

    // Use by Filter
    public JwtAuthenticationToken(String rawToken) {
        super(AuthorityUtils.NO_AUTHORITIES);
        this.rawToken = rawToken;
        this.principal = null;
        setAuthenticated(false);
    }

    // Use by Provider
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
