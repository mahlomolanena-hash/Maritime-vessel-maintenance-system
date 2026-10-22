package com.example.maintenance.service;

import com.example.maintenance.domain.RefreshToken;
import com.example.maintenance.domain.User;
import com.example.maintenance.exception.BadRequestException;
import com.example.maintenance.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * Refresh tokens are opaque random strings (not JWTs) — only a BCrypt hash is stored,
 * so a DB leak alone doesn't hand out valid sessions. Rotation: each refresh call
 * revokes the old token and issues a new one.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Transactional
    public String issue(User user) {
        String rawToken = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(BCrypt.hashpw(rawToken, BCrypt.gensalt(10)))
                .expiresAt(Instant.now().plus(refreshExpirationMs, ChronoUnit.MILLIS))
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional
    public User consumeAndRotate(String rawToken) {
        RefreshToken match = refreshTokenRepository.findAll().stream()
                .filter(t -> t.isValid() && BCrypt.checkpw(rawToken, t.getTokenHash()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Invalid or expired refresh token"));

        match.setRevoked(true);
        refreshTokenRepository.save(match);
        return match.getUser();
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
