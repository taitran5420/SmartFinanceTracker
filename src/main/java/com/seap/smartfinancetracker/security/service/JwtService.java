package com.seap.smartfinancetracker.security.service;

import com.seap.smartfinancetracker.security.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;
import java.util.function.Function;

public interface JwtService {
    String extractUsername(String token);
    <T> T extractClaim(String token, Function<Claims, T> claimsResolver);
    String generateToken(UserPrincipal userPrincipal);
    String generateToken(Map<String, Object> extraClaims, UserPrincipal userPrincipal);
    boolean validateToken(String token, UserPrincipal userPrincipal);
    long getExpirationTime();
}
