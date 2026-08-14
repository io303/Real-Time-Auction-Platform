package com.auction.platform.service.impl;

import com.auction.platform.dto.request.LoginRequest;
import com.auction.platform.dto.request.RefreshTokenRequest;
import com.auction.platform.dto.request.RegisterRequest;
import com.auction.platform.dto.response.AuthResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.entity.Role;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.RoleType;
import com.auction.platform.exception.DuplicateResourceException;
import com.auction.platform.exception.EmailNotVerifiedException;
import com.auction.platform.exception.RateLimitExceededException;
import com.auction.platform.mapper.UserMapper;
import com.auction.platform.repository.RoleRepository;
import com.auction.platform.repository.UserRepository;
import com.auction.platform.security.jwt.JwtService;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.AuthService;
import com.auction.platform.service.EmailVerificationService;
import com.auction.platform.service.RefreshTokenService;
import com.auction.platform.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final EmailVerificationService emailVerificationService;
    private final RefreshTokenService refreshTokenService;
    private final RateLimiterService rateLimiterService;

    @Override
    @Transactional
    public MessageResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + request.getEmail());
        }

        Role buyerRole = roleRepository.findByName(RoleType.ROLE_BUYER)
                .orElseThrow(() -> new IllegalStateException("ROLE_BUYER not seeded in database"));

        Set<Role> roles = new HashSet<>();
        roles.add(buyerRole);

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .roles(roles)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        emailVerificationService.sendVerification(savedUser);

        return MessageResponse.of("Registration successful. Please check your email to verify your account before logging in.");
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        if (!rateLimiterService.isAllowed("login:" + request.getEmail(), 5, 60)) {
            throw new RateLimitExceededException("Too many login attempts. Please try again in a minute.");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalStateException("User vanished after authentication"));

        if (!user.isEmailVerified()) {
            throw new EmailNotVerifiedException("Please verify your email before logging in.");
        }

        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.issueRefreshToken(user);

        return userMapper.toAuthResponse(user, accessToken, refreshToken);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        RefreshTokenService.RotatedToken rotated = refreshTokenService.rotate(request.getRefreshToken());
        User user = rotated.user();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = jwtService.generateAccessToken(userDetails);

        return userMapper.toAuthResponse(user, newAccessToken, rotated.newRawRefreshToken());
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }
}
