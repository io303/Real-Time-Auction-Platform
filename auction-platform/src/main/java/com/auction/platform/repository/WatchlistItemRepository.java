package com.auction.platform.repository;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.User;
import com.auction.platform.entity.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    boolean existsByUserAndAuction(User user, Auction auction);
    Optional<WatchlistItem> findByUserAndAuction(User user, Auction auction);
    List<WatchlistItem> findByUserOrderByCreatedAtDesc(User user);
    List<WatchlistItem> findByAuction(Auction auction);
}
