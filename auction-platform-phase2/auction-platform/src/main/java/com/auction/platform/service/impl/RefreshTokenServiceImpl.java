package com.auction.platform.service.impl;

import com.auction.platform.entity.RefreshToken;
import com.auction.platform.entity.User;
import com.auction.platform.exception.InvalidTokenException;
import com.auction.platform.exception.TokenExpiredException;
import com.auction.platform.repository.RefreshTokenRepository;
import com.auction.platform.security.jwt.JwtProperties;
import com.auction.platform.security.token.TokenHasher;
import com.auction.platform.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public String issueRefreshToken(User user) {
        String rawToken = TokenHasher.generateRawToken();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.hash(rawToken))
                .expiresAt(expiryFromNow())
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);
        return rawToken;
    }

    @Override
    @Transactional
    public RotatedToken rotate(String rawRefreshToken) {
        String hash = TokenHasher.hash(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (existing.isRevoked()) {
            // Reuse of an already-rotated token — likely theft. Nuke every session for this user.
            log.warn("Refresh token reuse detected for user {}. Revoking all sessions.", existing.getUser().getId());
            refreshTokenRepository.revokeAllActiveTokensForUser(existing.getUser());
            throw new InvalidTokenException("Refresh token reuse detected — all sessions revoked. Please log in again.");
        }

        if (existing.isExpired()) {
            throw new TokenExpiredException("Refresh token has expired. Please log in again.");
        }

        User user = existing.getUser();
        existing.setRevoked(true);

        String newRawToken = TokenHasher.generateRawToken();
        RefreshToken replacement = RefreshToken.builder()
                .user(user)
                .tokenHash(TokenHasher.hash(newRawToken))
                .expiresAt(expiryFromNow())
                .revoked(false)
                .build();

        refreshTokenRepository.save(replacement);
        existing.setReplacedByTokenId(replacement.getId());
        refreshTokenRepository.save(existing);

        return new RotatedToken(user, newRawToken);
    }

    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        String hash = TokenHasher.hash(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    @Override
    @Transactional
    public void revokeAllForUser(User user) {
        refreshTokenRepository.revokeAllActiveTokensForUser(user);
    }

    private LocalDateTime expiryFromNow() {
        return LocalDateTime.now().plusNanos(jwtProperties.getRefreshTokenExpirationMs() * 1_000_000);
    }
}
