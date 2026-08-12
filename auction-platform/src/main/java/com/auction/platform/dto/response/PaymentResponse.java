package com.auction.platform.dto.response;

import com.auction.platform.entity.enums.PaymentStatus;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private Long auctionId;
    private String auctionTitle;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String gatewayOrderId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
