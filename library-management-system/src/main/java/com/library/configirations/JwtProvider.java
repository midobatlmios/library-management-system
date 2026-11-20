package com.library.configirations;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Collectors;

/** Creates and verifies login tokens (JWT). */
@Slf4j
@Service
public class JwtProvider {

    private static final int MIN_SECRET_LENGTH = 32;

    @Value("${app.jwt.secret:}")
    private String secret;

    // 24 hours by default; change app.jwt.expiration-ms in application.properties.
    @Value("${app.jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey key;

    @PostConstruct
    void init() {
        if (secret == null || secret.isBlank()) {
            byte[] random = new byte[48];
            new SecureRandom().nextBytes(random);
            secret = Base64.getEncoder().encodeToString(random);
            log.warn("No JWT secret configured (app.jwt.secret / JWT_SECRET). Using a random one for this run: "
                    + "all login tokens will stop working when the app restarts. Set it in secrets.properties.");
        } else if (secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException("app.jwt.secret must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication authentication) {
        String roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));
        Date now = new Date();
        return Jwts.builder()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expirationMs))
                .claim("email", authentication.getName())
                .claim("authorities", roles)
                .signWith(key)
                .compact();
    }

    /** Verifies signature and expiry; throws if the token is invalid. Accepts "Bearer xxx" or "xxx". */
    public Claims parse(String jwt) {
        if (jwt.startsWith("Bearer ")) {
            jwt = jwt.substring(7);
        }
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(jwt).getPayload();
    }

    public String getEmailFromJwtToken(String jwt) {
        return String.valueOf(parse(jwt).get("email"));
    }
}
