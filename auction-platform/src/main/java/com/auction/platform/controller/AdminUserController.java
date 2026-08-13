package com.auction.platform.controller;

import com.auction.platform.dto.response.AdminUserResponse;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin - Users", description = "Admin-only user management")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List/search all users (admin only)")
    public ResponseEntity<ApiResponse<Page<AdminUserResponse>>> list(
            @RequestParam(required = false) String keyword, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Users fetched", adminUserService.listUsers(keyword, pageable)));
    }

    @PatchMapping("/{userId}/ban")
    @Operation(summary = "Ban a user — disables login and revokes active sessions immediately")
    public ResponseEntity<ApiResponse<MessageResponse>> ban(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long userId) {
        adminUserService.banUser(principal.getUser(), userId);
        return ResponseEntity.ok(ApiResponse.success("User banned", MessageResponse.of("User has been banned")));
    }

    @PatchMapping("/{userId}/unban")
    @Operation(summary = "Unban a user — restores login access")
    public ResponseEntity<ApiResponse<MessageResponse>> unban(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long userId) {
        adminUserService.unbanUser(principal.getUser(), userId);
        return ResponseEntity.ok(ApiResponse.success("User unbanned", MessageResponse.of("User has been unbanned")));
    }
}
