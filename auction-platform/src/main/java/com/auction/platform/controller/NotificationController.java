package com.auction.platform.controller;

import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.dto.response.NotificationResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notification history")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "List your notifications, paginated")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails principal, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Notifications fetched",
                notificationService.getMyNotifications(principal.getUser(), pageable)));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<MessageResponse>> markAsRead(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        notificationService.markAsRead(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success("Marked as read", MessageResponse.of("Notification marked as read")));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get count of unread notifications (for a badge/bell icon)")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Unread count fetched",
                Map.of("unread", notificationService.countUnread(principal.getUser()))));
    }
}
