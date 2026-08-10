package com.auction.platform.service;

import com.auction.platform.entity.Auction;

public interface AntiSnipeService {
    /** If auction.endDate falls within the configured window from now, extends it. Must be
     *  called while the caller already holds the auction's pessimistic lock. */
    void applyIfWithinWindow(Auction auction);
}
