package com.auction.platform.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformStatsResponse {
    private long totalUsers;
    private long totalBuyers;
    private long totalSellers;
    private long totalAdmins;
    private long newUsersLast7Days;
    private long totalAuctions;
    private Map<String, Long> auctionsByStatus;
    private long totalBids;
    private long totalPayments;
    private BigDecimal totalRevenue;
}
