package com.auction.platform.repository;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BidRepository extends JpaRepository<Bid, Long> {
    Page<Bid> findByAuctionOrderByCreatedAtDesc(Auction auction, Pageable pageable);
}
