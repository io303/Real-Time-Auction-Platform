package com.auction.platform.repository;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.AutoBid;
import com.auction.platform.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AutoBidRepository extends JpaRepository<AutoBid, Long> {
    Optional<AutoBid> findByAuctionAndBidder(Auction auction, User bidder);
    List<AutoBid> findByAuctionAndActiveTrue(Auction auction);
}
