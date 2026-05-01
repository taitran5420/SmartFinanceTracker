package com.seap.smartfinancetracker.security.service;

import com.seap.smartfinancetracker.security.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret}")
    private String jwtSecretKey;

    @Value("${jwt.expiration}")
    private long jwtExpirationInMs;

    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    @Override
    public String generateToken(UserPrincipal userPrincipal) {
        return generateToken(new HashMap<>(), userPrincipal);
    }

    @Override
    public String generateToken(Map<String, Object> extraClaims, UserPrincipal userPrincipal) {
        return buildToken(extraClaims, userPrincipal);
    }

    @Override
    public boolean validateToken(String token, UserPrincipal userPrincipal) {
        final String username = extractUsername(token);
        return (username.equals(userPrincipal.getUsername()) && !validateExpirationToken(token));
    }

    @Override
    public long getExpirationTime() {
        return jwtExpirationInMs;
    }

    private boolean validateExpirationToken(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private String buildToken(Map<String, Object> extraClaims, UserPrincipal userPrincipal) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("id", userPrincipal.getId().toString());

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
