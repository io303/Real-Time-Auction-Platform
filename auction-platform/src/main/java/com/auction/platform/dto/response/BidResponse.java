package com.auction.platform.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidResponse {

    private Long id;

    private Long auctionId;

    private String auctionTitle;

    private String bidderName;

    private BigDecimal amount;

    private LocalDateTime createdAt;
}