package com.auction.platform.service.impl;

import com.auction.platform.dto.response.NotificationResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Notification;
import com.auction.platform.entity.User;
import com.auction.platform.entity.WatchlistItem;
import com.auction.platform.entity.enums.NotificationType;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.repository.NotificationRepository;
import com.auction.platform.repository.WatchlistItemRepository;
import com.auction.platform.service.EmailService;
import com.auction.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final WatchlistItemRepository watchlistItemRepository;
    private final EmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public void notifyUser(User recipient, NotificationType type, String title, String message, Auction relatedAuction) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .auction(relatedAuction)
                .build();
        notificationRepository.save(notification);

        // Best-effort delivery on both channels — a failure here must never roll back the
        // notification record itself or the business operation that triggered it.
        try {
            emailService.sendNotificationEmail(recipient.getEmail(), title, message);
        } catch (Exception e) {
            log.warn("Failed to email notification to {}: {}", recipient.getEmail(), e.getMessage());
        }

        try {
            NotificationResponse payload = toResponse(notification);
            messagingTemplate.convertAndSendToUser(recipient.getEmail(), "/queue/notifications", payload);
        } catch (Exception e) {
            log.warn("Failed to push WebSocket notification to {}: {}", recipient.getEmail(), e.getMessage());
        }
    }

    @Override
    public void notifyWatchers(Auction auction, NotificationType type, String title, String message) {
        for (WatchlistItem item : watchlistItemRepository.findByAuction(auction)) {
            notifyUser(item.getUser(), type, title, message, auction);
        }
    }

    @Override
    public Page<NotificationResponse> getMyNotifications(User user, Pageable pageable) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public void markAsRead(User user, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Notification not found"); // don't reveal others' notifications exist
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public long countUnread(User user) {
        return notificationRepository.countByRecipientAndReadFalse(user);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .auctionId(n.getAuction() != null ? n.getAuction().getId() : null)
                .read(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
