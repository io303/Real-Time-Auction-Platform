package com.auction.platform.repository;

import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.RoleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Page<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullNameKeyword, String emailKeyword, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime cutoff);

    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.name = :roleType")
    long countByRole(RoleType roleType);
}
