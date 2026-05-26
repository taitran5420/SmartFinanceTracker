package com.seap.smartfinancetracker.security.mapper;

import com.seap.smartfinancetracker.security.model.UserPrincipal;
import com.seap.smartfinancetracker.user.entity.User;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * Component responsible for mapping a core {@link User} entity to a Spring Security {@link UserPrincipal}.
 * <p>
 * This mapper acts as a vital bridge between the application's domain model and Spring Security's
 * authentication mechanism, ensuring that user identity, credentials, and role-based authorities
 * are correctly translated and injected into the security context.
 * </p>
 */
@Component
public class UserPrincipalMapper {

    private static final String ROLE_PREFIX = "ROLE_";

    /**
     * Converts a domain {@link User} entity into a {@link UserPrincipal}.
     * <p>
     * <b>Security Implementation Note:</b> This method automatically prepends the mandatory
     * {@code "ROLE_"} prefix to the user's role enum. This ensures strict compliance with
     * Spring Security's default role-based authorization matchers (e.g., properly translating
     * a database role of {@code USER} into the authority {@code ROLE_USER}).
     * </p>
     *
     * @param user the user entity retrieved from the database
     * @return a fully populated {@link UserPrincipal} ready to be stored in the SecurityContext,
     * or {@code null} if the input user is null
     */
    public UserPrincipal toUserPrincipal(User user) {
        return UserPrincipal.builder()
                .id(user.getId())
                .email(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(ROLE_PREFIX + user.getRole().name())))
                .build();
    }
}
