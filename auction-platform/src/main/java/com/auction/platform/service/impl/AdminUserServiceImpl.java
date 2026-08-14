package com.auction.platform.service.impl;

import com.auction.platform.dto.response.AdminUserResponse;
import com.auction.platform.entity.User;
import com.auction.platform.exception.ApiException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.repository.UserRepository;
import com.auction.platform.security.userdetails.CustomUserDetailsService;
import com.auction.platform.service.AdminUserService;
import com.auction.platform.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    public Page<AdminUserResponse> listUsers(String keyword, Pageable pageable) {
        String search = keyword != null ? keyword : "";
        return userRepository
                .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(search, search, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void banUser(User admin, Long userId) {
        if (admin.getId().equals(userId)) {
            throw new ApiException("You cannot ban your own account", HttpStatus.BAD_REQUEST);
        }
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        target.setEnabled(false);
        userRepository.save(target);
        customUserDetailsService.evictCache(target.getEmail());

        // Immediately invalidate their active sessions — a ban should take effect right away,
        // not just block future logins.
        refreshTokenService.revokeAllForUser(target);
    }

    @Override
    @Transactional
    public void unbanUser(User admin, Long userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        target.setEnabled(true);
        userRepository.save(target);
        customUserDetailsService.evictCache(target.getEmail());
    }

    private AdminUserResponse toResponse(User user) {
        Set<String> roleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toSet());

        return AdminUserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .roles(roleNames)
                .enabled(user.isEnabled())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
