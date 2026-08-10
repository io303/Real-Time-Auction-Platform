package com.auction.platform.service.impl;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.service.AuctionBroadcastService;
import com.auction.platform.service.AuctionLifecycleTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionLifecycleTransitionServiceImpl implements AuctionLifecycleTransitionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBroadcastService auctionBroadcastService;

    @Override
    @Transactional
    public void tryStart(Long auctionId) {
        auctionRepository.findByIdForUpdate(auctionId).ifPresent(auction -> {
            // Re-check status + date AFTER acquiring the lock — the unlocked scan that found
            // this candidate could be stale by the time we actually get here.
            if (auction.getStatus() == AuctionStatus.SCHEDULED && !auction.getStartDate().isAfter(LocalDateTime.now())) {
                auction.setStatus(AuctionStatus.LIVE);
                Auction saved = auctionRepository.save(auction);
                auctionBroadcastService.broadcastAfterCommit(saved);
                log.info("Auction {} transitioned SCHEDULED -> LIVE", auctionId);
            }
        });
    }

    @Override
    @Transactional
    public void tryEnd(Long auctionId) {
        auctionRepository.findByIdForUpdate(auctionId).ifPresent(auction -> {
            // THIS recheck is what makes anti-sniping race-safe. If a bid extended endDate
            // between the unlocked scan and this lock being acquired, this condition will now
            // correctly evaluate false, and the auction stays LIVE.
            if (auction.getStatus() == AuctionStatus.LIVE && !auction.getEndDate().isAfter(LocalDateTime.now())) {
                auction.setStatus(AuctionStatus.ENDED);
                Auction saved = auctionRepository.save(auction);
                auctionBroadcastService.broadcastAfterCommit(saved);
                log.info("Auction {} transitioned LIVE -> ENDED", auctionId);
                // Notification hook (Phase 10): "auction ended, you won/lost" emails will be
                // triggered from here once the Notification service exists.
            }
        });
    }
}
