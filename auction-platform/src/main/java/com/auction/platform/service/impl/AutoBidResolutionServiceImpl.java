package com.auction.platform.service.impl;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.AutoBid;
import com.auction.platform.entity.Bid;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.NotificationType;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.AutoBidRepository;
import com.auction.platform.repository.BidRepository;
import com.auction.platform.service.AutoBidResolutionService;
import com.auction.platform.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AutoBidResolutionServiceImpl implements AutoBidResolutionService {

    private final AutoBidRepository autoBidRepository;
    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final NotificationService notificationService;

    @Override
    public void resolve(Auction auction) {
        List<AutoBid> activeAutoBids = autoBidRepository.findByAuctionAndActiveTrue(auction);

        // Safety cap so a pathological data state (shouldn't happen) can never infinite-loop.
        int maxIterations = activeAutoBids.size() + 1;

        // Dedup guard: an auto-bid war can bump the "previous highest bidder" several times in
        // one resolve() call. Each affected user should get at most one OUTBID notification per
        // call, not one per intermediate step.
        Set<Long> alreadyNotified = new HashSet<>();

        for (int i = 0; i < maxIterations; i++) {
            BigDecimal currentHighest = auction.getCurrentHighestBid() != null
                    ? auction.getCurrentHighestBid()
                    : auction.getStartingPrice();
            Long currentHighestBidderId = auction.getCurrentHighestBidder() != null
                    ? auction.getCurrentHighestBidder().getId()
                    : null;

            Optional<AutoBid> best = activeAutoBids.stream()
                    .filter(ab -> !ab.getBidder().getId().equals(currentHighestBidderId))
                    .filter(ab -> ab.getMaxBid().compareTo(currentHighest) > 0)
                    .max(Comparator.comparing(AutoBid::getMaxBid).thenComparing(Comparator.comparing(AutoBid::getCreatedAt).reversed()));

            if (best.isEmpty()) {
                break; // stable state reached
            }

            AutoBid winner = best.get();
            BigDecimal requiredBid = currentHighest.add(auction.getMinIncrement());
            BigDecimal newBidAmount = winner.getMaxBid().min(requiredBid);

            if (newBidAmount.compareTo(currentHighest) <= 0) {
                // This bidder's max can't clear the next increment — remove them from
                // consideration and let the next loop iteration try the next-best candidate.
                activeAutoBids.remove(winner);
                continue;
            }

            Bid systemBid = Bid.builder()
                    .auction(auction)
                    .bidder(winner.getBidder())
                    .amount(newBidAmount)
                    .build();
            bidRepository.save(systemBid);

            User previousHighestBidder = auction.getCurrentHighestBidder();

            auction.setCurrentHighestBid(newBidAmount);
            auction.setCurrentHighestBidder(winner.getBidder());

            if (previousHighestBidder != null
                    && !previousHighestBidder.getId().equals(winner.getBidder().getId())
                    && alreadyNotified.add(previousHighestBidder.getId())) {
                notificationService.notifyUser(previousHighestBidder, NotificationType.OUTBID,
                        "You've been outbid: " + auction.getTitle(),
                        "Someone placed a higher bid. Current highest: " + newBidAmount + ".",
                        auction);
            }
        }

        auctionRepository.save(auction);
    }
}
