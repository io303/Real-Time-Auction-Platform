package com.auction.platform.repository;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.AuctionImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuctionImageRepository extends JpaRepository<AuctionImage, Long> {

    List<AuctionImage> findByAuctionOrderByDisplayOrderAsc(Auction auction);

    Optional<AuctionImage> findByIdAndAuction(Long id, Auction auction);

    long countByAuction(Auction auction);

    @Modifying
    @Query("UPDATE AuctionImage ai SET ai.isPrimary = false WHERE ai.auction = :auction AND ai.isPrimary = true")
    void clearPrimaryForAuction(Auction auction);
}
