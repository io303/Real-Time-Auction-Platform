package com.auction.platform.service.impl;

import com.auction.platform.dto.request.CreateAuctionRequest;
import com.auction.platform.dto.request.UpdateAuctionRequest;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Category;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.entity.enums.NotificationType;
import com.auction.platform.entity.enums.PaymentStatus;
import com.auction.platform.exception.ApiException;
import com.auction.platform.exception.InvalidStateTransitionException;
import com.auction.platform.exception.OwnershipException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.mapper.AuctionMapper;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.CategoryRepository;
import com.auction.platform.repository.PaymentRepository;
import com.auction.platform.service.AuctionBroadcastService;
import com.auction.platform.service.AuctionCacheService;
import com.auction.platform.service.AuctionService;
import com.auction.platform.service.NotificationService;
import com.auction.platform.service.search.AuctionSearchCriteria;
import com.auction.platform.service.search.AuctionSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final CategoryRepository categoryRepository;
    private final AuctionMapper auctionMapper;
    private final PaymentRepository paymentRepository;
    private final AuctionBroadcastService auctionBroadcastService;
    private final NotificationService notificationService;
    private final AuctionCacheService auctionCacheService;

    private static final List<AuctionStatus> PUBLICLY_VISIBLE_STATUSES =
            List.of(AuctionStatus.SCHEDULED, AuctionStatus.LIVE, AuctionStatus.ENDED);

    @Override
    @Transactional
    public AuctionResponse create(User seller, CreateAuctionRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        validateDatesAndPrices(request.getStartDate(), request.getEndDate(),
                request.getStartingPrice(), request.getReservePrice());

        Auction auction = Auction.builder()
                .seller(seller)
                .category(category)
                .title(request.getTitle())
                .description(request.getDescription())
                .startingPrice(request.getStartingPrice())
                .reservePrice(request.getReservePrice())
                .minIncrement(request.getMinIncrement())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(AuctionStatus.DRAFT)
                .build();

        Auction saved = auctionRepository.save(auction);
        return auctionMapper.toResponse(saved, true);
    }

    @Override
    @Transactional
    public AuctionResponse update(User requester, Long auctionId, UpdateAuctionRequest request) {
        Auction auction = findOwnedOrThrow(requester, auctionId);

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Only DRAFT auctions can be edited. This auction is " + auction.getStatus());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        validateDatesAndPrices(request.getStartDate(), request.getEndDate(),
                request.getStartingPrice(), request.getReservePrice());

        auction.setTitle(request.getTitle());
        auction.setDescription(request.getDescription());
        auction.setCategory(category);
        auction.setStartingPrice(request.getStartingPrice());
        auction.setReservePrice(request.getReservePrice());
        auction.setMinIncrement(request.getMinIncrement());
        auction.setStartDate(request.getStartDate());
        auction.setEndDate(request.getEndDate());

        Auction saved = auctionRepository.save(auction);
        auctionCacheService.evict(auctionId);
        return auctionMapper.toResponse(saved, true);
    }

    @Override
    @Transactional
    public AuctionResponse publish(User requester, Long auctionId) {
        Auction auction = findOwnedOrThrow(requester, auctionId);

        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Only DRAFT auctions can be published. Current status: " + auction.getStatus());
        }

        validateDatesAndPrices(auction.getStartDate(), auction.getEndDate(),
                auction.getStartingPrice(), auction.getReservePrice());

        auction.setStatus(AuctionStatus.SCHEDULED);
        Auction saved = auctionRepository.save(auction);
        auctionCacheService.evict(auctionId);
        return auctionMapper.toResponse(saved, true);
    }

    @Override
    @Transactional
    public void delete(User requester, Long auctionId) {
        Auction auction = findOwnedOrThrow(requester, auctionId);

        if (auction.getStatus() == AuctionStatus.LIVE || auction.getStatus() == AuctionStatus.ENDED) {
            throw new InvalidStateTransitionException(
                    "Cannot delete an auction that is " + auction.getStatus() + ". Bidding data must be preserved.");
        }

        auctionRepository.delete(auction);
        auctionCacheService.evict(auctionId);
    }

    @Override
    public AuctionResponse getById(User requester, Long auctionId) {
        // Try cache first for the public view.
        Optional<AuctionResponse> cached = auctionCacheService.getCachedPublicView(auctionId);

        if (cached.isEmpty()) {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

            boolean isOwner = requester != null && auction.getSeller().getId().equals(requester.getId());
            boolean isAdmin = requester != null && requester.getRoles().stream()
                    .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));

            if (auction.getStatus() == AuctionStatus.DRAFT && !isOwner && !isAdmin) {
                throw new ResourceNotFoundException("Auction not found");
            }

            AuctionResponse publicView = auctionMapper.toResponse(auction, false);
            // Only cache non-draft auctions — drafts are private and shouldn't sit in a shared cache.
            if (auction.getStatus() != AuctionStatus.DRAFT) {
                auctionCacheService.putPublicView(auctionId, publicView);
            }

            return isOwner || isAdmin ? auctionMapper.toResponse(auction, true) : publicView;
        }

        // Cache hit: we still need to know if THIS caller is privileged, to decide whether to
        // overlay reservePrice — but we avoid a full entity load for the common (public) case.
        boolean isAdmin = requester != null && requester.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
        boolean isOwner = false;

        if (requester != null && !isAdmin) {
            // Cheap check: is this requester the seller? One lightweight query, not a full mapping.
            isOwner = auctionRepository.findById(auctionId)
                    .map(a -> a.getSeller().getId().equals(requester.getId()))
                    .orElse(false);
        }

        if (!isOwner && !isAdmin) {
            return cached.get(); // fast path — no DB hit at all for the vast majority of public views
        }

        // Privileged viewer on a cache hit: fetch just enough to overlay reservePrice.
        Auction full = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
        AuctionResponse privileged = cached.get();
        privileged.setReservePrice(full.getReservePrice());
        return privileged;
    }

    @Override
    public Page<AuctionResponse> listPublished(Pageable pageable) {
        return auctionRepository.findByStatusIn(PUBLICLY_VISIBLE_STATUSES, pageable)
                .map(auction -> auctionMapper.toResponse(auction, false));
    }

    @Override
    public List<AuctionResponse> listMine(User seller) {
        return auctionRepository.findBySellerOrderByCreatedAtDesc(seller).stream()
                .map(auction -> auctionMapper.toResponse(auction, true))
                .toList();
    }

    @Override
    public Page<AuctionResponse> search(AuctionSearchCriteria criteria, Pageable pageable) {
        AuctionStatus requestedStatus = null;
        if (criteria.getStatus() != null) {
            try {
                requestedStatus = AuctionStatus.valueOf(criteria.getStatus().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new ApiException("Invalid status filter: " + criteria.getStatus(), HttpStatus.BAD_REQUEST);
            }
        }

        // Privacy boundary: intersect the requested status with what's actually publicly
        // visible — a caller can never widen this to see DRAFT auctions via search filters.
        List<AuctionStatus> effectiveStatuses = requestedStatus != null && PUBLICLY_VISIBLE_STATUSES.contains(requestedStatus)
                ? List.of(requestedStatus)
                : PUBLICLY_VISIBLE_STATUSES;

        Specification<Auction> spec = Specification
                .where(AuctionSpecifications.hasKeyword(criteria.getKeyword()))
                .and(AuctionSpecifications.hasCategory(criteria.getCategoryId()))
                .and(AuctionSpecifications.hasSeller(criteria.getSellerId()))
                .and(AuctionSpecifications.priceAtLeast(criteria.getMinPrice()))
                .and(AuctionSpecifications.priceAtMost(criteria.getMaxPrice()))
                .and(AuctionSpecifications.hasStatusIn(effectiveStatuses));

        return auctionRepository.findAll(spec, pageable)
                .map(auction -> auctionMapper.toResponse(auction, false));
    }

    @Override
    @Transactional
    public AuctionResponse adminCancel(User admin, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new InvalidStateTransitionException("Auction is already cancelled");
        }
        if (auction.getStatus() == AuctionStatus.ENDED) {
            boolean hasSuccessfulPayment = !paymentRepository
                    .findByAuctionAndStatusIn(auction, List.of(PaymentStatus.SUCCESS))
                    .isEmpty();
            if (hasSuccessfulPayment) {
                throw new InvalidStateTransitionException(
                        "Cannot cancel an auction with a completed payment. Use refund instead.");
            }
        }

        auction.setStatus(AuctionStatus.CANCELLED);
        Auction saved = auctionRepository.save(auction);
        auctionCacheService.evict(auctionId);
        auctionBroadcastService.broadcastAfterCommit(saved);

        notificationService.notifyWatchers(saved, NotificationType.AUCTION_CANCELLED,
                "Auction removed: " + saved.getTitle(),
                "This auction was removed by an administrator.");

        if (saved.getCurrentHighestBidder() != null) {
            notificationService.notifyUser(saved.getCurrentHighestBidder(), NotificationType.AUCTION_CANCELLED,
                    "Auction removed: " + saved.getTitle(),
                    "An auction you were winning was removed by an administrator. No payment is required.",
                    saved);
        }

        log.info("Admin {} cancelled auction {}", admin.getId(), auctionId);
        return auctionMapper.toResponse(saved, true);
    }

    private Auction findOwnedOrThrow(User requester, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        boolean isOwner = auction.getSeller().getId().equals(requester.getId());

        if (!isOwner) {
            if (auction.getStatus() == AuctionStatus.DRAFT) {
                throw new ResourceNotFoundException("Auction not found");
            }
            throw new OwnershipException("You do not have permission to modify this auction");
        }

        return auction;
    }

    private void validateDatesAndPrices(LocalDateTime startDate, LocalDateTime endDate,
                                         BigDecimal startingPrice, BigDecimal reservePrice) {
        if (!endDate.isAfter(startDate)) {
            throw new ApiException("End date must be after start date", HttpStatus.BAD_REQUEST);
        }
        if (reservePrice != null && reservePrice.compareTo(startingPrice) < 0) {
            throw new ApiException("Reserve price cannot be lower than the starting price", HttpStatus.BAD_REQUEST);
        }
    }
}
