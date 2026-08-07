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
import com.auction.platform.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final VerificationTokenRepository verificationTokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${app.email.verification-token-expiration-minutes}")
    private long expirationMinutes;

    @Override
    @Transactional
    public void sendVerification(User user) {
        String rawToken = TokenHasher.generateRawToken();

        VerificationToken token = VerificationToken.builder()
                .user(user)
                .tokenHash(TokenHasher.hash(rawToken))
                .type(TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusMinutes(expirationMinutes))
                .build();

        verificationTokenRepository.save(token);
        emailService.sendVerificationEmail(user.getEmail(), rawToken);
    }

    @Override
    @Transactional
    public void verify(String rawToken) {
        String hash = TokenHasher.hash(rawToken);
        VerificationToken token = verificationTokenRepository
                .findByTokenHashAndType(hash, TokenType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new InvalidTokenException("Invalid verification token"));

        if (token.isUsed()) {
            throw new InvalidTokenException("This verification link has already been used");
        }
        if (token.isExpired()) {
            throw new TokenExpiredException("Verification link has expired. Please request a new one.");
        }

        User user = token.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);

        token.setUsedAt(LocalDateTime.now());
        verificationTokenRepository.save(token);
    }
}
