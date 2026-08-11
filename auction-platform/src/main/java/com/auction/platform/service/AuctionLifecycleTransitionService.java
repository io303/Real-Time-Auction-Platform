package com.auction.platform.service;

public interface AuctionLifecycleTransitionService {
    void tryStart(Long auctionId);
    void tryEnd(Long auctionId);
    void checkEndingSoon(Long auctionId);
}
