package com.asusoftware.SafeDocs_api.service;

import com.asusoftware.SafeDocs_api.domain.RefreshToken;
import com.asusoftware.SafeDocs_api.domain.User;
import com.asusoftware.SafeDocs_api.repo.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository repo;
    @Value("${app.security.jwt.refreshTokenTtlDays}") int ttlDays;

    private static final SecureRandom RNG = new SecureRandom();

    public RefreshToken mint(User user) {
        byte[] bytes = new byte[48]; // 64 chars Base64
        RNG.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        var rt = RefreshToken.builder()
                .user(user)
                .token(token)
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(ttlDays, ChronoUnit.DAYS))
                .build();
        return repo.save(rt);
    }

    public RefreshToken requireValid(String token) {
        var rt = repo.findByToken(token).orElseThrow(() -> new IllegalArgumentException("invalid_refresh_token"));
        if (rt.getRevokedAt() != null) throw new IllegalArgumentException("refresh_token_revoked");
        if (rt.getExpiresAt().isBefore(Instant.now())) throw new IllegalArgumentException("refresh_token_expired");
        return rt;
    }

    public void revoke(RefreshToken rt) {
        rt.setRevokedAt(Instant.now());
        repo.save(rt);
    }

    public void revokeAllForUser(java.util.UUID userId) {
        repo.deleteByUserId(userId);
    }
}