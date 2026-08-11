package com.auction.platform.service.impl;

import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.User;
import com.auction.platform.entity.WatchlistItem;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.exception.ApiException;
import com.auction.platform.exception.DuplicateResourceException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.mapper.AuctionMapper;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.WatchlistItemRepository;
import com.auction.platform.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {

    private final WatchlistItemRepository watchlistItemRepository;
    private final AuctionRepository auctionRepository;
    private final AuctionMapper auctionMapper;

    @Override
    @Transactional
    public void addToWatchlist(User user, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (auction.getStatus() == AuctionStatus.DRAFT) {
            throw new ApiException("Cannot watch an unpublished auction", HttpStatus.BAD_REQUEST);
        }
        if (watchlistItemRepository.existsByUserAndAuction(user, auction)) {
            throw new DuplicateResourceException("This auction is already in your watchlist");
        }

        watchlistItemRepository.save(WatchlistItem.builder().user(user).auction(auction).build());
    }

    @Override
    @Transactional
    public void removeFromWatchlist(User user, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        WatchlistItem item = watchlistItemRepository.findByUserAndAuction(user, auction)
                .orElseThrow(() -> new ResourceNotFoundException("This auction is not in your watchlist"));

        watchlistItemRepository.delete(item);
    }

    @Override
    public List<AuctionResponse> getMyWatchlist(User user) {
        return watchlistItemRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(item -> auctionMapper.toResponse(item.getAuction(), false))
                .toList();
    }
}
