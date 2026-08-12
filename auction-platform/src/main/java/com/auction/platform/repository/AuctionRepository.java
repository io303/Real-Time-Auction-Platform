package com.auction.platform.repository;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Category;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long>, JpaSpecificationExecutor<Auction> {

    Optional<Auction> findByIdAndSeller(Long id, User seller);

    List<Auction> findBySellerOrderByCreatedAtDesc(User seller);

    Page<Auction> findByStatusIn(List<AuctionStatus> statuses, Pageable pageable);

    boolean existsByCategory(Category category);

    /**
     * Locks the auction row for the duration of the transaction — used exclusively for bid
     * placement so concurrent bids on the same auction are serialized, not racing each other.
     * Also reused by the Phase 9 scheduler to avoid a scheduler-vs-bid race condition.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Auction a WHERE a.id = :id")
    Optional<Auction> findByIdForUpdate(Long id);

    @Query("SELECT a.id FROM Auction a WHERE a.status = :status AND a.startDate <= :now")
    List<Long> findIdsByStatusAndStartDateBefore(AuctionStatus status, LocalDateTime now);

    @Query("SELECT a.id FROM Auction a WHERE a.status = :status AND a.endDate <= :now")
    List<Long> findIdsByStatusAndEndDateBefore(AuctionStatus status, LocalDateTime now);

    @Query("SELECT a.id FROM Auction a WHERE a.status = :status AND a.endDate <= :cutoff AND a.endingSoonNotified = false")
    List<Long> findIdsForEndingSoonNotification(AuctionStatus status, LocalDateTime cutoff);
}
