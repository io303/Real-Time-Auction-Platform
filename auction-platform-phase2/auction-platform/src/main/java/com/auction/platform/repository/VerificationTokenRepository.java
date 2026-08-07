package com.auction.platform.repository;

import com.auction.platform.entity.VerificationToken;
import com.auction.platform.entity.enums.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByTokenHashAndType(String tokenHash, TokenType type);
}
