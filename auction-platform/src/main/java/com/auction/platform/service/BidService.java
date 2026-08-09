package com.auction.platform.service;

import com.auction.platform.dto.request.PlaceBidRequest;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.dto.response.BidResponse;
import com.auction.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BidService {
    AuctionResponse placeBid(User bidder, Long auctionId, PlaceBidRequest request);
    Page<BidResponse> getBidHistory(Long auctionId, Pageable pageable);

    /** Temporary manual override until Phase 9's scheduler auto-transitions SCHEDULED -> LIVE. */
    AuctionResponse forceLive(User requester, Long auctionId);
}
