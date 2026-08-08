package com.auction.platform.repository;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Category;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    Optional<Auction> findByIdAndSeller(Long id, User seller);

    List<Auction> findBySellerOrderByCreatedAtDesc(User seller);

    Page<Auction> findByStatusIn(List<AuctionStatus> statuses, Pageable pageable);

    boolean existsByCategory(Category category);
}
