package com.seap.smartfinancetracker.security.service;

import com.seap.smartfinancetracker.security.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.*;

/**
 * Implementation of JWT service for JWT token generation, parsing, and validation.
 */
@Slf4j
@Service
public class JwtServiceImpl implements JwtService {

    public static final String ID_CLAIMS_KEY = "id";
    public static final String ROLE_CLAIMS_KEY = "roles";

    @Value("${jwt.secret}")
    private String jwtSecretKey;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    @Override
    public String generateToken(UserPrincipal userPrincipal) {
        return generateToken(new HashMap<>(), userPrincipal);
    }

    @Override
    public String generateToken(Map<String, Object> extraClaims, UserPrincipal userPrincipal) {
        return buildToken(extraClaims, userPrincipal);
    }

    @Override
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (JwtException e) {
            log.warn("JWT Validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>Implementation Details:</b>
     * This method extracts standard and custom claims directly from the validated JWT.
     * By utilizing custom claims ({@code ID_CLAIMS_KEY} and {@code ROLE_CLAIMS_KEY}),
     * it reconstructs the complete {@link UserPrincipal} without requiring any subsequent
     * database lookups. This approach maximizes the performance of the stateless authentication filter.
     * </p>
     */
    @Override
    public UserPrincipal extractUserPrincipal(String token) {
        Claims claims = getClaimsFromToken(token);

        String email = claims.getSubject();
        String idStr = claims.get(ID_CLAIMS_KEY, String.class);
        String role = claims.get(ROLE_CLAIMS_KEY, String.class);

        return UserPrincipal.builder()
                .id(UUID.fromString(idStr))
                .email(email)
                .authorities(Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
    }

    @Override
    public long getExpirationTime() {
        return jwtExpirationInMs;
    }

    private String buildToken(Map<String, Object> extraClaims, UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(ID_CLAIMS_KEY, userPrincipal.getId().toString());

        if (!userPrincipal.getAuthorities().isEmpty()) {
            claims.put(ROLE_CLAIMS_KEY, userPrincipal.getAuthorities().iterator().next().getAuthority());
        }

        return Jwts.builder()
                .claims(claims)
                .subject(userPrincipal.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationInMs))
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    private Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSecretKey() {
        byte[] encodedKey = Base64.getDecoder().decode(jwtSecretKey);
        return Keys.hmacShaKeyFor(encodedKey);
    }
}
