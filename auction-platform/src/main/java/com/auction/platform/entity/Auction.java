package com.auction.platform.entity;

import com.auction.platform.entity.enums.AuctionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "auctions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Auction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "starting_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal startingPrice;

    @Column(name = "reserve_price", precision = 12, scale = 2)
    private BigDecimal reservePrice;

    @Column(name = "min_increment", nullable = false, precision = 12, scale = 2)
    private BigDecimal minIncrement;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private AuctionStatus status = AuctionStatus.DRAFT;

    @Column(name = "current_highest_bid", precision = 12, scale = 2)
    private BigDecimal currentHighestBid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_highest_bidder_id")
    private User currentHighestBidder;

    @OneToMany(mappedBy = "auction", fetch = FetchType.LAZY)
    private List<AuctionImage> images;

    @Builder.Default
    @Column(name = "ending_soon_notified", nullable = false)
    private boolean endingSoonNotified = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
