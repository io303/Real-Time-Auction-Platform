package com.auction.platform.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BidResponse {
    private Long id;
    private String bidderName;
    private BigDecimal amount;
    private LocalDateTime createdAt;
}
