package com.auction.platform.service;

import com.auction.platform.dto.request.CreateAuctionRequest;
import com.auction.platform.dto.request.UpdateAuctionRequest;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.User;
import com.auction.platform.service.search.AuctionSearchCriteria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AuctionService {
    AuctionResponse create(User seller, CreateAuctionRequest request);
    AuctionResponse update(User requester, Long auctionId, UpdateAuctionRequest request);
    AuctionResponse publish(User requester, Long auctionId);
    void delete(User requester, Long auctionId);
    AuctionResponse getById(User requester, Long auctionId);
    Page<AuctionResponse> listPublished(Pageable pageable);
    List<AuctionResponse> listMine(User seller);
    Page<AuctionResponse> search(AuctionSearchCriteria criteria, Pageable pageable);
}
