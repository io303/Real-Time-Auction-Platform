package com.auction.platform.service;

import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.User;

import java.util.List;

public interface WatchlistService {
    void addToWatchlist(User user, Long auctionId);
    void removeFromWatchlist(User user, Long auctionId);
    List<AuctionResponse> getMyWatchlist(User user);
}
