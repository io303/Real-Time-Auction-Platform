package com.auction.platform.service;

import com.auction.platform.dto.request.SetAutoBidRequest;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.dto.response.AutoBidResponse;
import com.auction.platform.entity.User;

public interface AutoBidService {
    AuctionResponse setAutoBid(User bidder, Long auctionId, SetAutoBidRequest request);
    void cancelAutoBid(User bidder, Long auctionId);
    AutoBidResponse getMyAutoBid(User bidder, Long auctionId);
}
