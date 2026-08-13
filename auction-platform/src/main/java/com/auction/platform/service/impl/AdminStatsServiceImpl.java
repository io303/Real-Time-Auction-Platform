package com.auction.platform.service.impl;

import com.auction.platform.dto.response.PlatformStatsResponse;
import com.auction.platform.entity.enums.RoleType;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.BidRepository;
import com.auction.platform.repository.PaymentRepository;
import com.auction.platform.repository.UserRepository;
import com.auction.platform.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminStatsServiceImpl implements AdminStatsService {

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public PlatformStatsResponse getPlatformStats() {
        Map<String, Long> auctionsByStatus = new HashMap<>();
        for (Object[] row : auctionRepository.countGroupedByStatus()) {
            auctionsByStatus.put(row[0].toString(), (Long) row[1]);
        }

        return PlatformStatsResponse.builder()
                .totalUsers(userRepository.count())
                .totalBuyers(userRepository.countByRole(RoleType.ROLE_BUYER))
                .totalSellers(userRepository.countByRole(RoleType.ROLE_SELLER))
                .totalAdmins(userRepository.countByRole(RoleType.ROLE_ADMIN))
                .newUsersLast7Days(userRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(7)))
                .totalAuctions(auctionRepository.count())
                .auctionsByStatus(auctionsByStatus)
                .totalBids(bidRepository.count())
                .totalPayments(paymentRepository.count())
                .totalRevenue(paymentRepository.sumSuccessfulPaymentAmounts())
                .build();
    }
}
