package com.auction.platform.controller;

import com.auction.platform.dto.request.ChangePasswordRequest;
import com.auction.platform.dto.request.UpdateProfileRequest;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.dto.response.UserProfileResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "Authenticated user's own profile, password, and image")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal CustomUserDetails principal) {
        UserProfileResponse response = profileService.getProfile(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", response));
    }

    @PutMapping
    @Operation(summary = "Update full name and phone number")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserProfileResponse response = profileService.updateProfile(principal.getUser(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", response));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password (requires current password)")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        profileService.changePassword(principal.getUser(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed",
                MessageResponse.of("Password updated. Other sessions have been logged out.")));
    }

    @PostMapping("/profile-image")
    @Operation(summary = "Upload a profile image (JPEG/PNG/WEBP, max 5MB)")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProfileImage(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam("file") MultipartFile file) {
        String url = profileService.uploadProfileImage(principal.getUser(), file);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded", Map.of("profileImageUrl", url)));
    }

    @PostMapping("/become-seller")
    @Operation(summary = "Self-service: grant yourself the Seller role (MVP simplification — no approval flow yet)")
    public ResponseEntity<ApiResponse<MessageResponse>> becomeSeller(
            @AuthenticationPrincipal CustomUserDetails principal) {
        profileService.becomeSeller(principal.getUser());
        return ResponseEntity.ok(ApiResponse.success("Role updated",
                MessageResponse.of("You can now create auctions as a seller. Call /auth/refresh-token to get an updated access token.")));
    }
}
