package com.rideflow.demo.security;

import com.rideflow.demo.config.ApplicationSecurityProperties;
import com.rideflow.demo.domain.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String USER_ID_CLAIM = "user_id";
    private static final String ROLE_CLAIM = "role";
    private static final String FULL_NAME_CLAIM = "full_name";

    private final ApplicationSecurityProperties securityProperties;
    private final SecretKey signingKey;

    public JwtService(ApplicationSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
        this.signingKey = Keys.hmacShaKeyFor(
            securityProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8)
        );
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(securityProperties.getAccessTokenExpiration());

        return Jwts.builder()
            .subject(user.id.toString())
            .claim(TOKEN_TYPE_CLAIM, TokenType.ACCESS.name())
            .claim(USER_ID_CLAIM, user.id.toString())
            .claim(ROLE_CLAIM, user.role.name())
            .claim(FULL_NAME_CLAIM, user.fullName)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey)
            .compact();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiration = now.plus(securityProperties.getRefreshTokenExpiration());

        return Jwts.builder()
            .subject(user.id.toString())
            .claim(TOKEN_TYPE_CLAIM, TokenType.REFRESH.name())
            .claim(USER_ID_CLAIM, user.id.toString())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiration))
            .signWith(signingKey)
            .compact();
    }

    public Long extractUserId(String token) {
        String userId = parseClaims(token).get(USER_ID_CLAIM, String.class);
        return Long.valueOf(userId);
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public boolean isTokenValid(String token, RideFlowUserPrincipal principal, TokenType tokenType) {
        Claims claims = parseClaims(token);
        String userId = claims.get(USER_ID_CLAIM, String.class);
        return principal.getUserId().toString().equals(userId)
            && tokenType.name().equals(claims.get(TOKEN_TYPE_CLAIM, String.class))
            && claims.getExpiration().toInstant().isAfter(Instant.now());
    }

    public boolean isTokenOfType(String token, TokenType tokenType) {
        return tokenType.name().equals(parseClaims(token).get(TOKEN_TYPE_CLAIM, String.class));
    }

    public long getAccessTokenExpirationSeconds() {
        return securityProperties.getAccessTokenExpiration().toSeconds();
    }

    public long getRefreshTokenExpirationSeconds() {
        return securityProperties.getRefreshTokenExpiration().toSeconds();
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
