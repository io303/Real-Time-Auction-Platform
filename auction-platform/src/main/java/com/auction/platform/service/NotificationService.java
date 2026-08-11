package com.auction.platform.service;

import com.auction.platform.dto.response.NotificationResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationService {
    /** Persists the notification, then best-effort pushes it via email and WebSocket. */
    void notifyUser(User recipient, NotificationType type, String title, String message, Auction relatedAuction);

    void notifyWatchers(Auction auction, NotificationType type, String title, String message);

    Page<NotificationResponse> getMyNotifications(User user, Pageable pageable);
    void markAsRead(User user, Long notificationId);
    long countUnread(User user);
}
