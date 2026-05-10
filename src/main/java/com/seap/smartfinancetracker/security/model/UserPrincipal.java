package com.seap.smartfinancetracker.security.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

/**
 * Represents the authenticated user details used by Spring Security.
 *
 * <p>This class acts as the application's security principal and is
 * stored in the Spring Security context after successful authentication.</p>
 */
@Getter
@Builder
@AllArgsConstructor
public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;

    @Builder.Default
    private final String password = null;
    private final Collection<? extends GrantedAuthority> authorities;

    @Override
    public @NonNull String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

}
