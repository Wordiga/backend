package com.wordiga.global.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;
    private final JwtParser parser;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-expiration:3600000}") long accessExpiration,
            @Value("${jwt.refresh-expiration:2592000000}") long refreshExpiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
        this.parser = Jwts.parser().verifyWith(key).build();
    }

    public String create(Long memberId) {
        return create(memberId, "access", accessExpiration);
    }

    public String createRefresh(Long memberId) {
        return create(memberId, "refresh", refreshExpiration);
    }

    private String create(Long memberId, String type, long expiration) {
        return Jwts.builder()
                .subject(memberId.toString())
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(key)
                .compact();
    }

    public Long getMemberId(String token) {
        return Long.parseLong(
                parser.parseSignedClaims(token).getPayload().getSubject()
        );
    }

    public boolean isValidAccess(String token) {
        return isValid(token, "access");
    }

    public boolean isValidRefresh(String token) {
        return isValid(token, "refresh");
    }

    private boolean isValid(String token, String type) {
        try {
            return type.equals(parser.parseSignedClaims(token).getPayload().get("type", String.class));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public long getRefreshExpiration() {
        return refreshExpiration;
    }
}
