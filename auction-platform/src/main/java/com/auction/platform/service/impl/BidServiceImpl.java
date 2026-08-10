package com.auction.platform.service.impl;

import com.auction.platform.dto.request.PlaceBidRequest;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.dto.response.BidResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Bid;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.exception.AuctionNotActiveException;
import com.auction.platform.exception.BidTooLowException;
import com.auction.platform.exception.InvalidStateTransitionException;
import com.auction.platform.exception.OwnershipException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.exception.SelfBidException;
import com.auction.platform.mapper.AuctionMapper;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.BidRepository;
import com.auction.platform.service.AntiSnipeService;
import com.auction.platform.service.AuctionBroadcastService;
import com.auction.platform.service.AutoBidResolutionService;
import com.auction.platform.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class BidServiceImpl implements BidService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionMapper auctionMapper;
    private final AutoBidResolutionService autoBidResolutionService;
    private final AuctionBroadcastService auctionBroadcastService;
    private final AntiSnipeService antiSnipeService;

    @Override
    @Transactional
    public AuctionResponse placeBid(User bidder, Long auctionId, PlaceBidRequest request) {
        // Pessimistic lock: this SELECT ... FOR UPDATE blocks any other concurrent bid on the
        // SAME auction until this transaction commits/rolls back. This is what makes bid
        // placement safe under high contention — two simultaneous bids can never both read the
        // same "current highest" value and both succeed.
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new AuctionNotActiveException("This auction is not currently accepting bids. Status: " + auction.getStatus());
        }

        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new SelfBidException("You cannot bid on your own auction");
        }

        BigDecimal minimumAcceptableBid = calculateMinimumAcceptableBid(auction);
        if (request.getAmount().compareTo(minimumAcceptableBid) < 0) {
            throw new BidTooLowException("Bid must be at least " + minimumAcceptableBid);
        }

        Bid bid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(request.getAmount())
                .build();
        bidRepository.save(bid);

        auction.setCurrentHighestBid(request.getAmount());
        auction.setCurrentHighestBidder(bidder);
        Auction saved = auctionRepository.save(auction);

        // Auto-Bid resolution (Phase 7): after a manual bid, check whether any active
        // auto-bidder needs to be counter-raised — same lock, same transaction.
        autoBidResolutionService.resolve(saved);
        antiSnipeService.applyIfWithinWindow(saved);
        auctionBroadcastService.broadcastAfterCommit(saved);

        // Note: no WebSocket push here yet — that's Phase 8. Bidders currently need to poll
        // GET /api/v1/auctions/{id} or GET /api/v1/auctions/{id}/bids to see updates.
        return auctionMapper.toResponse(saved, isOwnerOrAdmin(bidder, saved));
    }

    @Override
    public Page<BidResponse> getBidHistory(Long auctionId, Pageable pageable) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        return bidRepository.findByAuctionOrderByCreatedAtDesc(auction, pageable)
                .map(bid -> BidResponse.builder()
                        .id(bid.getId())
                        .bidderName(bid.getBidder().getFullName())
                        .amount(bid.getAmount())
                        .createdAt(bid.getCreatedAt())
                        .build());
    }

    @Override
    @Transactional
    public AuctionResponse forceLive(User requester, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        boolean isOwner = auction.getSeller().getId().equals(requester.getId());
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
        if (!isOwner && !isAdmin) {
            throw new OwnershipException("You do not have permission to change this auction's status");
        }

        if (auction.getStatus() != AuctionStatus.SCHEDULED) {
            throw new InvalidStateTransitionException("Only SCHEDULED auctions can be forced live. Current: " + auction.getStatus());
        }

        auction.setStatus(AuctionStatus.LIVE);
        Auction saved = auctionRepository.save(auction);
        return auctionMapper.toResponse(saved, true);
    }

    private BigDecimal calculateMinimumAcceptableBid(Auction auction) {
        if (auction.getCurrentHighestBid() == null) {
            return auction.getStartingPrice();
        }
        return auction.getCurrentHighestBid().add(auction.getMinIncrement());
    }

    private boolean isOwnerOrAdmin(User user, Auction auction) {
        boolean isOwner = auction.getSeller().getId().equals(user.getId());
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
        return isOwner || isAdmin;
    }
}
