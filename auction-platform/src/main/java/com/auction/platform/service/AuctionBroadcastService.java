package com.auction.platform.service;

import com.auction.platform.entity.Auction;

public interface AuctionBroadcastService {
    /** Schedules a broadcast of the auction's current public state, deferred until after the
     *  enclosing transaction (if any) commits — never before, to avoid phantom updates on rollback. */
    void broadcastAfterCommit(Auction auction);
}
