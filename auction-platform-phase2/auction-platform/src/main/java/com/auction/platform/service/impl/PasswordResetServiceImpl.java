package com.auction.platform.service.impl;

import com.auction.platform.entity.User;
import com.auction.platform.entity.VerificationToken;
import com.auction.platform.entity.enums.TokenType;
import com.auction.platform.exception.InvalidTokenException;
import com.auction.platform.exception.TokenExpiredException;
import com.auction.platform.repository.UserRepository;
import com.auction.platform.repository.VerificationTokenRepository;
import com.auction.platform.security.token.TokenHasher;
import com.auction.platform.service.EmailService;
import com.auction.platform.service.PasswordResetService;
import com.auction.platform.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.email.password-reset-token-expiration-minutes}")
    private long expirationMinutes;

    @Override
    @Transactional
    public void initiateReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String rawToken = TokenHasher.generateRawToken();

            VerificationToken token = VerificationToken.builder()
                    .user(user)
                    .tokenHash(TokenHasher.hash(rawToken))
                    .type(TokenType.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                    .build();

            verificationTokenRepository.save(token);
            emailService.sendPasswordResetEmail(user.getEmail(), rawToken);
        });
        // Intentionally no else-branch: response is identical whether or not the email exists.
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = TokenHasher.hash(rawToken);
        VerificationToken token = verificationTokenRepository
                .findByTokenHashAndType(hash, TokenType.PASSWORD_RESET)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        if (token.isUsed()) {
            throw new InvalidTokenException("This reset link has already been used");
        }
        if (token.isExpired()) {
            throw new TokenExpiredException("Reset link has expired. Please request a new one.");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(token);

        // Changing a password should invalidate every existing session — force re-login everywhere.
        refreshTokenService.revokeAllForUser(user);
        log.info("Password reset completed for user {}. All refresh tokens revoked.", user.getId());
    }
}
