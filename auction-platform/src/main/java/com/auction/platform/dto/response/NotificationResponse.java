package com.auction.platform.dto.response;

import com.auction.platform.entity.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String message;
    private Long auctionId;
    private boolean read;
    private LocalDateTime createdAt;
}
