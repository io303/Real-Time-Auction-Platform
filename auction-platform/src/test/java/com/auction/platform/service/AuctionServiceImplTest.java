package com.auction.platform.service;

import com.auction.platform.dto.request.UpdateAuctionRequest;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Category;
import com.auction.platform.entity.Role;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.entity.enums.RoleType;
import com.auction.platform.exception.InvalidStateTransitionException;
import com.auction.platform.exception.OwnershipException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.mapper.AuctionMapper;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.CategoryRepository;
import com.auction.platform.repository.PaymentRepository;
import com.auction.platform.service.impl.AuctionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the Phase 4 auction state machine and the Phase 4 ownership/visibility rules
 * (404-for-private-draft vs 403-for-published-but-unauthorized).
 */
class AuctionServiceImplTest {

    @Mock private AuctionRepository auctionRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private AuctionMapper auctionMapper;
    @Mock private PaymentRepository paymentRepository;
    @Mock private AuctionBroadcastService auctionBroadcastService;
    @Mock private NotificationService notificationService;
    @Mock private AuctionCacheService auctionCacheService;

    private AuctionServiceImpl auctionService;

    private User owner;
    private User stranger;
    private Auction draftAuction;
    private Auction liveAuction;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        auctionService = new AuctionServiceImpl(
                auctionRepository, categoryRepository, auctionMapper, paymentRepository,
                auctionBroadcastService, notificationService, auctionCacheService);

        Role buyerRole = Role.builder().id(1L).name(RoleType.ROLE_BUYER).build();
        owner = User.builder().id(1L).fullName("Owner").roles(Set.of(buyerRole)).build();
        stranger = User.builder().id(2L).fullName("Stranger").roles(Set.of(buyerRole)).build();

        Category category = Category.builder().id(1L).name("Electronics").build();

        draftAuction = Auction.builder()
                .id(10L).seller(owner).category(category)
                .title("Draft item").startingPrice(new BigDecimal("100"))
                .minIncrement(new BigDecimal("5"))
                .startDate(LocalDateTime.now().plusDays(1))
                .endDate(LocalDateTime.now().plusDays(2))
                .status(AuctionStatus.DRAFT)
                .build();

        liveAuction = Auction.builder()
                .id(11L).seller(owner).category(category)
                .title("Live item").startingPrice(new BigDecimal("100"))
                .minIncrement(new BigDecimal("5"))
                .startDate(LocalDateTime.now().minusHours(1))
                .endDate(LocalDateTime.now().plusHours(1))
                .status(AuctionStatus.LIVE)
                .build();
    }

    @Test
    void getById_draftAuction_nonOwnerGets404NotFound() {
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(draftAuction));
        when(auctionCacheService.getCachedPublicView(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> auctionService.getById(stranger, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getById_draftAuction_ownerCanSeeIt() {
        when(auctionRepository.findById(10L)).thenReturn(Optional.of(draftAuction));
        when(auctionCacheService.getCachedPublicView(10L)).thenReturn(Optional.empty());
        when(auctionMapper.toResponse(eq(draftAuction), eq(true)))
                .thenReturn(com.auction.platform.dto.response.AuctionResponse.builder().id(10L).build());

        var response = auctionService.getById(owner, 10L);

        assertThat(response.getId()).isEqualTo(10L);
    }

    @Test
    void update_onPublishedAuction_throwsInvalidStateTransition() {
        UpdateAuctionRequest request = new UpdateAuctionRequest();
        request.setCategoryId(1L);
        request.setStartDate(LocalDateTime.now().plusDays(1));
        request.setEndDate(LocalDateTime.now().plusDays(2));
        request.setStartingPrice(new BigDecimal("100"));
        request.setMinIncrement(new BigDecimal("5"));
        request.setTitle("Attempted edit");
        request.setDescription("desc");

        when(auctionRepository.findById(11L)).thenReturn(Optional.of(liveAuction));

        assertThatThrownBy(() -> auctionService.update(owner, 11L, request))
                .isInstanceOf(InvalidStateTransitionException.class);
    }

    @Test
    void update_byNonOwnerOnLiveAuction_throws403Ownership_not404() {
        UpdateAuctionRequest request = new UpdateAuctionRequest();
        request.setCategoryId(1L);
        request.setStartDate(LocalDateTime.now().plusDays(1));
        request.setEndDate(LocalDateTime.now().plusDays(2));
        request.setStartingPrice(new BigDecimal("100"));
        request.setMinIncrement(new BigDecimal("5"));
        request.setTitle("x");
        request.setDescription("y");

        when(auctionRepository.findById(11L)).thenReturn(Optional.of(liveAuction));

        // Published (non-DRAFT) + non-owner => existence is already public knowledge,
        // so this must be 403 (OwnershipException), NOT 404.
        assertThatThrownBy(() -> auctionService.update(stranger, 11L, request))
                .isInstanceOf(OwnershipException.class);
    }

    @Test
    void update_byNonOwnerOnDraftAuction_throws404_not403() {
        UpdateAuctionRequest request = new UpdateAuctionRequest();
        request.setCategoryId(1L);
        request.setStartDate(LocalDateTime.now().plusDays(1));
        request.setEndDate(LocalDateTime.now().plusDays(2));
        request.setStartingPrice(new BigDecimal("100"));
        request.setMinIncrement(new BigDecimal("5"));
        request.setTitle("x");
        request.setDescription("y");

        when(auctionRepository.findById(10L)).thenReturn(Optional.of(draftAuction));

        // Draft + non-owner => privacy boundary, must be 404 (pretend it doesn't exist).
        assertThatThrownBy(() -> auctionService.update(stranger, 10L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void publish_onNonDraftAuction_throwsInvalidStateTransition() {
        when(auctionRepository.findById(11L)).thenReturn(Optional.of(liveAuction));

        assertThatThrownBy(() -> auctionService.publish(owner, 11L))
                .isInstanceOf(InvalidStateTransitionException.class);
    }
}
