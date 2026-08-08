package com.auction.platform.service.impl;

import com.auction.platform.dto.request.ChangePasswordRequest;
import com.auction.platform.dto.request.UpdateProfileRequest;
import com.auction.platform.dto.response.AddressResponse;
import com.auction.platform.dto.response.UserProfileResponse;
import com.auction.platform.entity.User;
import com.auction.platform.exception.WrongPasswordException;
import com.auction.platform.mapper.AddressMapper;
import com.auction.platform.repository.AddressRepository;
import com.auction.platform.repository.UserRepository;
import com.auction.platform.service.ProfileImageStorageService;
import com.auction.platform.service.ProfileService;
import com.auction.platform.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final ProfileImageStorageService profileImageStorageService;

    @Override
    public UserProfileResponse getProfile(User user) {
        return buildProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(User user, UpdateProfileRequest request) {
        user.setFullName(request.getFullName());
        user.setPhoneNumber(request.getPhoneNumber());
        User saved = userRepository.save(user);
        return buildProfileResponse(saved);
    }

    @Override
    @Transactional
    public void changePassword(User user, ChangePasswordRequest request) {
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new WrongPasswordException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Same principle as Phase 2 reset-password: a password change should invalidate other sessions.
        refreshTokenService.revokeAllForUser(user);
    }

    @Override
    @Transactional
    public String uploadProfileImage(User user, MultipartFile file) {
        String relativeUrl = profileImageStorageService.store(file, user.getId());
        user.setProfileImageUrl(relativeUrl);
        userRepository.save(user);
        return relativeUrl;
    }

    private UserProfileResponse buildProfileResponse(User user) {
        List<AddressResponse> addresses =
                addressRepository.findByUserOrderByIsDefaultDescCreatedAtAsc(user).stream()
                        .map(addressMapper::toResponse)
                        .collect(Collectors.toList());

        Set<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .profileImageUrl(user.getProfileImageUrl())
                .emailVerified(user.isEmailVerified())
                .roles(roleNames)
                .addresses(addresses)
                .build();
    }
}
