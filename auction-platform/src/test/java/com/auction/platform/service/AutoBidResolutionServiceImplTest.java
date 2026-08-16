package com.auction.platform.service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.AutoBid;
import com.auction.platform.entity.User;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.AutoBidRepository;
import com.auction.platform.repository.BidRepository;
import com.auction.platform.service.impl.AutoBidResolutionServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;



/**
 * Unit tests for the Phase 7 proxy-bidding algorithm — pure logic, dependencies mocked,
 * no database involved. This directly tests the eBay-style "system only bids as much as
 * needed" resolution loop described in the Phase 7 design doc.
 */
class AutoBidResolutionServiceImplTest {

    @Mock
    private AutoBidRepository autoBidRepository;
    @Mock private BidRepository bidRepository;
    @Mock private AuctionRepository auctionRepository;
    @Mock private NotificationService notificationService;

    private AutoBidResolutionServiceImpl resolutionService;

    private Auction auction;
    private User sellerUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        resolutionService = new AutoBidResolutionServiceImpl(
                autoBidRepository, bidRepository, auctionRepository, notificationService);

        sellerUser = User.builder().id(1L).fullName("Seller").build();
        auction = Auction.builder()
                .id(100L)
                .seller(sellerUser)
                .startingPrice(new BigDecimal("100.00"))
                .minIncrement(new BigDecimal("5.00"))
                .currentHighestBid(null)
                .currentHighestBidder(null)
                .build();
    }

    private User buyer(long id, String name) {
        return User.builder().id(id).fullName(name).build();
    }

    private AutoBid autoBid(User bidder, String maxBid, LocalDateTime createdAt) {
        AutoBid ab = AutoBid.builder()
                .auction(auction)
                .bidder(bidder)
                .maxBid(new BigDecimal(maxBid))
                .active(true)
                .build();
        ab.setCreatedAt(createdAt);
        return ab;
    }

    @Test
    void resolve_singleActiveAutoBidder_raisesOnlyToStartingPriceOrMinimum() {
        User buyerA = buyer(2L, "Buyer A");
        AutoBid autoBidA = autoBid(buyerA, "200.00", LocalDateTime.now());

        when(autoBidRepository.findByAuctionAndActiveTrue(auction)).thenReturn(List.of(autoBidA));

        resolutionService.resolve(auction);

        // No competitor exists, so the system should NOT reveal buyerA's full max (200) —
        // it should only raise them to the point where they're already the highest, i.e.
        // the starting price, since nobody has forced a raise yet.
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo("100.00");
        assertThat(auction.getCurrentHighestBidder()).isEqualTo(buyerA);
    }

    @Test
    void resolve_classicEbayScenario_raisesOnlyEnoughToBeatCompetitor() {
        // Buyer A has a 200 max auto-bid already active and winning at starting price.
        User buyerA = buyer(2L, "Buyer A");
        AutoBid autoBidA = autoBid(buyerA, "200.00", LocalDateTime.now().minusMinutes(5));
        auction.setCurrentHighestBid(new BigDecimal("100.00"));
        auction.setCurrentHighestBidder(buyerA);

        when(autoBidRepository.findByAuctionAndActiveTrue(auction)).thenReturn(List.of(autoBidA));

        // Buyer B manually bids 150 (simulated by the caller updating auction before resolve()
        // — this mirrors what BidServiceImpl does right before calling resolve()).
        User buyerB = buyer(3L, "Buyer B");
        auction.setCurrentHighestBid(new BigDecimal("150.00"));
        auction.setCurrentHighestBidder(buyerB);

        resolutionService.resolve(auction);

        // System should counter-raise buyerA to exactly 155 (150 + 5 increment) — NOT to 200.
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo("155.00");
        assertThat(auction.getCurrentHighestBidder()).isEqualTo(buyerA);
    }

    @Test
    void resolve_competitorExceedsAutoBidMax_competitorWins() {
        User buyerA = buyer(2L, "Buyer A");
        AutoBid autoBidA = autoBid(buyerA, "200.00", LocalDateTime.now());
        when(autoBidRepository.findByAuctionAndActiveTrue(auction)).thenReturn(List.of(autoBidA));

        User buyerB = buyer(3L, "Buyer B");
        auction.setCurrentHighestBid(new BigDecimal("200.00")); // B bids exactly A's max
        auction.setCurrentHighestBidder(buyerB);

        resolutionService.resolve(auction);

        // A's max (200) is not GREATER than the current highest (200) — A cannot counter-raise.
        assertThat(auction.getCurrentHighestBid()).isEqualByComparingTo("200.00");
        assertThat(auction.getCurrentHighestBidder()).isEqualTo(buyerB);
        verify(bidRepository, never()).save(any());
    }

    @Test
    void resolve_tieBetweenTwoAutoBidders_earlierSubmissionWins() {
        User buyerA = buyer(2L, "Buyer A (earlier)");
        User buyerB = buyer(3L, "Buyer B (later)");

        AutoBid autoBidA = autoBid(buyerA, "300.00", LocalDateTime.now().minusHours(1));
        AutoBid autoBidB = autoBid(buyerB, "300.00", LocalDateTime.now());

        when(autoBidRepository.findByAuctionAndActiveTrue(auction)).thenReturn(List.of(autoBidA, autoBidB));

        resolutionService.resolve(auction);

        assertThat(auction.getCurrentHighestBidder()).isEqualTo(buyerA);
    }

    @Test
    void resolve_noActiveAutoBids_leavesAuctionUnchanged() {
        when(autoBidRepository.findByAuctionAndActiveTrue(auction)).thenReturn(List.of());

        resolutionService.resolve(auction);

        assertThat(auction.getCurrentHighestBid()).isNull();
        assertThat(auction.getCurrentHighestBidder()).isNull();
        verify(bidRepository, never()).save(any());
    }
}
