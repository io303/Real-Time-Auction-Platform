package com.auction.platform.repository;

import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Payment;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);
    List<Payment> findByAuctionAndStatusIn(Auction auction, List<PaymentStatus> statuses);
    Page<Payment> findByBuyerOrderByCreatedAtDesc(User buyer, Pageable pageable);
}
