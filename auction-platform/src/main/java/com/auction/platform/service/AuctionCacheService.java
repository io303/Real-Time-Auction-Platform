package com.auction.platform.service;

import com.auction.platform.dto.response.AuctionResponse;

import java.util.Optional;

public interface AuctionCacheService {
    Optional<AuctionResponse> getCachedPublicView(Long auctionId);
    void putPublicView(Long auctionId, AuctionResponse publicResponse);
    void evict(Long auctionId);
}
