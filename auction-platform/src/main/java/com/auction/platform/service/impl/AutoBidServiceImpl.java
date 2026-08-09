package com.auction.platform.service.impl;

import com.auction.platform.dto.request.SetAutoBidRequest;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.dto.response.AutoBidResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.AutoBid;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.exception.AuctionNotActiveException;
import com.auction.platform.exception.InvalidAutoBidException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.exception.SelfBidException;
import com.auction.platform.mapper.AuctionMapper;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.AutoBidRepository;
import com.auction.platform.service.AutoBidResolutionService;
import com.auction.platform.service.AutoBidService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AutoBidServiceImpl implements AutoBidService {

    private final AuctionRepository auctionRepository;
    private final AutoBidRepository autoBidRepository;
    private final AutoBidResolutionService resolutionService;
    private final AuctionMapper auctionMapper;

    @Override
    @Transactional
    public AuctionResponse setAutoBid(User bidder, Long auctionId, SetAutoBidRequest request) {
        // Same lock used by manual bidding (Phase 6) — this auto-bid update is itself a
        // bid-affecting mutation and must be serialized against concurrent manual/auto bids.
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (auction.getStatus() != AuctionStatus.LIVE) {
            throw new AuctionNotActiveException("This auction is not currently accepting bids. Status: " + auction.getStatus());
        }
        if (auction.getSeller().getId().equals(bidder.getId())) {
            throw new SelfBidException("You cannot bid on your own auction");
        }

        BigDecimal currentHighest = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid() : auction.getStartingPrice();
        if (request.getMaxBid().compareTo(currentHighest) <= 0) {
            throw new InvalidAutoBidException("Max bid must be higher than the current highest bid (" + currentHighest + ")");
        }

        AutoBid autoBid = autoBidRepository.findByAuctionAndBidder(auction, bidder)
                .orElse(AutoBid.builder().auction(auction).bidder(bidder).build());
        autoBid.setMaxBid(request.getMaxBid());
        autoBid.setActive(true);
        autoBidRepository.save(autoBid);

        resolutionService.resolve(auction);

        boolean isOwnerOrAdmin = false; // bidders never see reservePrice via this endpoint
        return auctionMapper.toResponse(auction, isOwnerOrAdmin);
    }

    @Override
    @Transactional
    public void cancelAutoBid(User bidder, Long auctionId) {
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        AutoBid autoBid = autoBidRepository.findByAuctionAndBidder(auction, bidder)
                .orElseThrow(() -> new ResourceNotFoundException("No active auto-bid found for this auction"));

        autoBid.setActive(false);
        autoBidRepository.save(autoBid);
        // No resolution re-run needed — deactivating only stops FUTURE auto-raises;
        // bids already placed on this bidder's behalf remain valid history.
    }

    @Override
    public AutoBidResponse getMyAutoBid(User bidder, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        AutoBid autoBid = autoBidRepository.findByAuctionAndBidder(auction, bidder)
                .orElseThrow(() -> new ResourceNotFoundException("No auto-bid found for this auction"));

        return AutoBidResponse.builder()
                .id(autoBid.getId())
                .maxBid(autoBid.getMaxBid())
                .active(autoBid.isActive())
                .updatedAt(autoBid.getUpdatedAt())
                .build();
    }
}
