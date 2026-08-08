package com.auction.platform.service;

import com.auction.platform.dto.request.ChangePasswordRequest;
import com.auction.platform.dto.request.UpdateProfileRequest;
import com.auction.platform.dto.response.UserProfileResponse;
import com.auction.platform.entity.User;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    UserProfileResponse getProfile(User user);
    UserProfileResponse updateProfile(User user, UpdateProfileRequest request);
    void changePassword(User user, ChangePasswordRequest request);
    String uploadProfileImage(User user, MultipartFile file);
    void becomeSeller(User user);
}
