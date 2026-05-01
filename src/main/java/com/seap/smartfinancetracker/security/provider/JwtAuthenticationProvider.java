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

@Component
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationProvider(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

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

    @Override
    public boolean supports(@NonNull Class<?> authentication) {
        return JwtAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
