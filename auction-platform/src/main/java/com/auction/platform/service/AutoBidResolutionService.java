package com.auction.platform.service;

import com.auction.platform.entity.Auction;

public interface AutoBidResolutionService {
    /**
     * Runs the proxy-bidding resolution loop against the given auction (which must already be
     * locked by the caller via findByIdForUpdate). Mutates the auction's currentHighestBid /
     * currentHighestBidder and persists any system-generated Bid rows as needed.
     */
    void resolve(Auction auction);
}
