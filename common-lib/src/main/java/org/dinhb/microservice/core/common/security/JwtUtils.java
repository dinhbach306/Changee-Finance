package org.dinhb.microservice.core.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class JwtUtils {

    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_ROLES = "roles";

    private final SecretKey signingKey;
    private final String issuer;
    private final Duration ttl;

    public JwtUtils(String secret, String issuer, Duration ttl) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 characters");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public String generate(UUID userId, String username, List<String> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(username)
                .claim(CLAIM_USER_ID, userId.toString())
                .claim(CLAIM_ROLES, roles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public Claims parse(String token) {
        Jws<Claims> jws = Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(issuer)
                .build()
                .parseSignedClaims(token);
        return jws.getPayload();
    }

    @SuppressWarnings("unchecked")
    public UserContext toContext(Claims claims) {
        UUID userId = UUID.fromString((String) claims.get(CLAIM_USER_ID));
        String username = claims.getSubject();
        List<String> roles = (List<String>) claims.getOrDefault(CLAIM_ROLES, List.of());
        return new UserContext(userId, username, roles);
    }

    public Map<String, Object> readClaims(String token) {
        return parse(token);
    }

    public record UserContext(UUID userId, String username, List<String> roles) {}
}
