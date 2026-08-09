package com.auction.platform.repository;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Category;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Optional<Auction> findByIdAndSeller(Long id, User seller);

    List<Auction> findBySellerOrderByCreatedAtDesc(User seller);

    Page<Auction> findByStatusIn(List<AuctionStatus> statuses, Pageable pageable);

    boolean existsByCategory(Category category);

    /**
     * Locks the auction row for the duration of the transaction — used exclusively for bid
     * placement so concurrent bids on the same auction are serialized, not racing each other.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdForUpdate(Long id);
}
