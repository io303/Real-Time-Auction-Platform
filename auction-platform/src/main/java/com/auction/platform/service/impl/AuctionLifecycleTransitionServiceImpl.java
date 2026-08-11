package com.auction.platform.service.impl;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.entity.enums.NotificationType;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.service.AuctionBroadcastService;
import com.auction.platform.service.AuctionLifecycleTransitionService;
import com.auction.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionLifecycleTransitionServiceImpl implements AuctionLifecycleTransitionService {

    private final AuctionRepository auctionRepository;
    private final AuctionBroadcastService auctionBroadcastService;
    private final NotificationService notificationService;

    @Value("${app.notification.ending-soon-minutes}")
    private long endingSoonMinutes;

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
                notificationService.notifyWatchers(saved, NotificationType.AUCTION_STARTED,
                        "Auction started: " + saved.getTitle(),
                        "An auction you're watching has started and is now accepting bids.");
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

                if (saved.getCurrentHighestBidder() != null) {
                    notificationService.notifyUser(saved.getCurrentHighestBidder(), NotificationType.WINNER,
                            "You won: " + saved.getTitle(),
                            "Congratulations! You won this auction at " + saved.getCurrentHighestBid() + ".",
                            saved);
                }
                log.info("Auction {} transitioned LIVE -> ENDED", auctionId);
                // Payment hook (Phase 11) goes here next.
            }
        });
    }

    @Override
    @Transactional
    public void checkEndingSoon(Long auctionId) {
        auctionRepository.findByIdForUpdate(auctionId).ifPresent(auction -> {
            if (auction.getStatus() == AuctionStatus.LIVE && !auction.isEndingSoonNotified()) {
                auction.setEndingSoonNotified(true);
                auctionRepository.save(auction);
                notificationService.notifyWatchers(auction, NotificationType.AUCTION_ENDING,
                        "Ending soon: " + auction.getTitle(),
                        "An auction you're watching ends in less than " + endingSoonMinutes + " minutes.");
            }
        });
    }
}
