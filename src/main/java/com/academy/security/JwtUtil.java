package com.academy.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

//Clase S4
/**
 * Signs and reads the HS-family JWT. The key length decides the algorithm: the 64-character
 * default secret is 512 bits, so jjwt picks HS512.
 */
@Component
@Slf4j
public class JwtUtil {

    private static final Duration EXPIRATION = Duration.ofHours(1);

    private static final String CLAIM_ROLES = "roles";
    private static final String CLAIM_USERNAME = "username";

    private final SecretKey key;

    public JwtUtil(@Value("${jjwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put(CLAIM_ROLES, user.getRoles());
        claims.put(CLAIM_USERNAME, user.getUsername());
        claims.put("test-value", "sample-test-value");

        Date issuedAt = new Date();
        Date expiration = Date.from(issuedAt.toInstant().plus(EXPIRATION));

        return Jwts.builder()
                .claims(claims)
                .subject(user.getUsername())
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    /**
     * Verifies the signature and the {@code exp} claim; a tampered or expired token throws.
     */
    public Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        return getAllClaimsFromToken(token).getSubject();
    }

    /**
     * @return {@code false} for anything jjwt refuses — bad signature, malformed, expired.
     */
    public boolean validateToken(String token) {
        try {
            getAllClaimsFromToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected token: {}", ex.toString());
            return false;
        }
    }

    /**
     * The {@code roles} claim comes back as a raw {@code List}, so it is narrowed here once
     * instead of at every call site.
     */
    public List<String> getRolesFromToken(String token) {
        Object roles = getAllClaimsFromToken(token).get(CLAIM_ROLES);
        return roles instanceof List<?> values
                ? values.stream().map(String::valueOf).toList()
                : List.of();
    }
}
