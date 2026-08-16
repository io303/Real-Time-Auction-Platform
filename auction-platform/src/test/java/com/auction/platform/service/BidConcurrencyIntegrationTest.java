package com.auction.platform.service;

import com.auction.platform.dto.request.PlaceBidRequest;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Category;
import com.auction.platform.entity.Role;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.entity.enums.RoleType;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.CategoryRepository;
import com.auction.platform.repository.RoleRepository;
import com.auction.platform.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The single most important test in this suite: proves the Phase 6 pessimistic-lock design
 * actually prevents a "lost update" when two bids race on the same auction simultaneously.
 *
 * Uses a real (H2) database + real transactions — this cannot be validated with Mockito, since
 * the entire point is testing genuine concurrent-transaction behavior against SELECT ... FOR
 * UPDATE, not mocked method calls.
 *
 * Note: H2's MVCC locking engine is not byte-for-byte identical to MySQL/InnoDB's row-locking
 * under extreme concurrency. This test gives strong confidence for local/CI runs; for
 * production-parity certainty, the natural next step (flagged in Phase 16 design notes) is a
 * Testcontainers-based variant of this same test against a real MySQL container.
 *
 * Also note: this test requires a live Spring context including Redis-backed beans
 * (AuctionCacheService, RateLimiterService — Phase 14). Both are designed to fail SOFT if
 * Redis is unreachable (cache-miss / rate-limit-open respectively), so this test passes
 * whether or not a local Redis instance is actually running.
 */
@SpringBootTest
@ActiveProfiles("test")
class BidConcurrencyIntegrationTest {

    @Autowired private AuctionRepository auctionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private BidService bidService;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void concurrentBids_onSameAuction_neverLoseAnUpdate() throws InterruptedException {
        Role buyerRole = roleRepository.findByName(RoleType.ROLE_BUYER).orElseThrow();

        User seller = userRepository.save(User.builder()
                .fullName("Concurrency Seller").email("concurrency-seller@example.com")
                .password(passwordEncoder.encode("Password1!"))
                .emailVerified(true).roles(java.util.Set.of(buyerRole)).build());

        Category category = categoryRepository.save(Category.builder()
                .name("Concurrency Test Category").slug("concurrency-test-category").build());

        Auction auction = auctionRepository.save(Auction.builder()
                .seller(seller).category(category)
                .title("Concurrency Test Auction").description("desc")
                .startingPrice(new BigDecimal("100.00"))
                .minIncrement(new BigDecimal("1.00"))
                .startDate(LocalDateTime.now().minusMinutes(1))
                .endDate(LocalDateTime.now().plusHours(1))
                .status(AuctionStatus.LIVE)
                .build());

        int bidderCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(bidderCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(bidderCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < bidderCount; i++) {
            final int bidderIndex = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // all threads wait here, then release together

                    User bidder = userRepository.save(User.builder()
                            .fullName("Bidder " + bidderIndex)
                            .email("concurrency-bidder-" + bidderIndex + "@example.com")
                            .password(passwordEncoder.encode("Password1!"))
                            .emailVerified(true).roles(java.util.Set.of(buyerRole)).build());

                    PlaceBidRequest request = new PlaceBidRequest();
                    // Every bidder attempts the SAME amount — only one can possibly "win" this
                    // exact value under correct locking; without locking, multiple could
                    // incorrectly succeed by reading a stale currentHighestBid.
                    request.setAmount(new BigDecimal("101.00"));

                    bidService.placeBid(bidder, auction.getId(), request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // Expected for all but one bidder — BidTooLowException once the first
                    // bid at 101.00 has been accepted and the minimum has moved past it.
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // release all threads at once
        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(completed).isTrue();
        // Exactly one bid at exactly 101.00 should have succeeded — proving no two threads
        // ever both read stale state and both "won" simultaneously.
        assertThat(successCount.get()).isEqualTo(1);

        Auction finalState = auctionRepository.findById(auction.getId()).orElseThrow();
        assertThat(finalState.getCurrentHighestBid()).isEqualByComparingTo("101.00");
    }
}
