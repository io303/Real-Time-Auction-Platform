package com.auction.platform.service.impl;

import com.auction.platform.dto.request.CreateAuctionRequest;
import com.auction.platform.dto.request.UpdateAuctionRequest;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Category;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.exception.ApiException;
import com.auction.platform.exception.InvalidStateTransitionException;
import com.auction.platform.exception.OwnershipException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.mapper.AuctionMapper;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.CategoryRepository;
import com.auction.platform.service.AuctionService;
import com.auction.platform.service.search.AuctionSearchCriteria;
import com.auction.platform.service.search.AuctionSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final CategoryRepository categoryRepository;
    private final AuctionMapper auctionMapper;

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
    }

    @Override
    public AuctionResponse getById(User requester, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        boolean isOwner = requester != null && auction.getSeller().getId().equals(requester.getId());
        boolean isAdmin = requester != null && requester.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));

        if (auction.getStatus() == AuctionStatus.DRAFT && !isOwner && !isAdmin) {
            throw new ResourceNotFoundException("Auction not found");
        }

        return auctionMapper.toResponse(auction, isOwner || isAdmin);
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
