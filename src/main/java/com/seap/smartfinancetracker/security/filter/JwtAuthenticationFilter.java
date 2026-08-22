package com.seap.smartfinancetracker.security.filter;

import com.seap.smartfinancetracker.security.token.JwtAuthenticationToken;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter responsible for processing JWT authentication for every incoming HTTP request.
 *
 * <p>This filter:
 * <ul>
 *     <li>Extracts JWT token from the Authorization header, falling back to a
 *     {@code ?token=} query param (needed for EventSource, which can't set headers)</li>
 *     <li>Validates the token through the AuthenticationManager</li>
 *     <li>If valid, stores the authenticated user in SecurityContextHolder</li>
 *     <li>If invalid, clears the security context and continues the filter chain</li>
 * </ul>
 *
 * <p>If no JWT token is provided, the request is passed through without authentication.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthenticationManager authenticationManager;

    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_QUERY_PARAM = "token";

    /**
     * Creates a new JWT authentication filter
     *
     * @param authenticationManager the authentication manager used to validate and authenticate JWT tokens
     */
    public JwtAuthenticationFilter(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    /**
     * Performs JWT authentication for the current HTTP request.
     * <p>
     * The filter checks the {@code Authorization} header for a Bearer token,
     * falling back to a {@code ?token=} query param if the header is absent.
     * If a valid JWT token is present, authentication is delegated to the
     * {@link AuthenticationManager}. Upon successful authentication, the
     * resulting {@link Authentication} object is stored in the
     * {@link SecurityContextHolder}.
     * <p>
     * If authentication fails or the header is missing/invalid, the request
     * proceeds without an authenticated user.
     *
     * @param request the incoming HTTP request
     * @param response the outgoing HTTP response
     * @param filterChain the filter chain used to pass the request and response to the next filter
     * @throws ServletException if the request cannot be processed
     * @throws IOException if an input or output error occurs during filtering
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
        final String jwtToken;

        if (authorizationHeader != null && authorizationHeader.startsWith(BEARER_TOKEN_PREFIX)) {
            jwtToken = authorizationHeader.substring(BEARER_TOKEN_PREFIX.length());
        } else {
            // Falls back to a ?token= query param when there's no Authorization
            // header — needed for EventSource (used by the SSE notification
            // stream), which can't set custom headers.
            jwtToken = request.getParameter(TOKEN_QUERY_PARAM);
        }

        if (jwtToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication authentication = new JwtAuthenticationToken(jwtToken);

            Authentication authenticationResult = authenticationManager.authenticate(authentication);

            SecurityContextHolder.getContext().setAuthentication(authenticationResult);
        } catch (AuthenticationException ex) {
            SecurityContextHolder.getContext().setAuthentication(null);
        }

        filterChain.doFilter(request, response);
    }
}
