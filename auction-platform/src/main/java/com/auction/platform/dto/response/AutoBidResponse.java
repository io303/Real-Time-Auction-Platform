package com.auction.platform.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutoBidResponse {
    private Long id;
    private BigDecimal maxBid;
    private boolean active;
    private LocalDateTime updatedAt;
}
