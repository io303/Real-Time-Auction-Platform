package com.auction.platform.service.impl;

import com.auction.platform.dto.response.AuctionImageResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.AuctionImage;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.exception.ApiException;
import com.auction.platform.exception.InvalidStateTransitionException;
import com.auction.platform.exception.OwnershipException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.repository.AuctionImageRepository;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.service.AuctionImageService;
import com.auction.platform.service.ProfileImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuctionImageServiceImpl implements AuctionImageService {

    private static final int MAX_IMAGES_PER_AUCTION = 8;

    private final AuctionImageRepository auctionImageRepository;
    private final AuctionRepository auctionRepository;
    private final ProfileImageStorageService storageService; // reused from Phase 3

    @Override
    @Transactional
    public List<AuctionImageResponse> addImage(User owner, Long auctionId, MultipartFile file) {
        Auction auction = findOwnedDraftOrThrow(owner, auctionId);

        if (auctionImageRepository.countByAuction(auction) >= MAX_IMAGES_PER_AUCTION) {
            throw new ApiException("An auction can have at most " + MAX_IMAGES_PER_AUCTION + " images",
                    HttpStatus.BAD_REQUEST);
        }

        String relativeUrl = storageService.store(file, auction.getId());
        boolean isFirstImage = auctionImageRepository.countByAuction(auction) == 0;

        AuctionImage image = AuctionImage.builder()
                .auction(auction)
                .imageUrl(relativeUrl)
                .isPrimary(isFirstImage)
                .displayOrder((int) auctionImageRepository.countByAuction(auction))
                .build();

        auctionImageRepository.save(image);

        return auctionImageRepository.findByAuctionOrderByDisplayOrderAsc(auction).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteImage(User owner, Long auctionId, Long imageId) {
        Auction auction = findOwnedDraftOrThrow(owner, auctionId);
        AuctionImage image = auctionImageRepository.findByIdAndAuction(imageId, auction)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        boolean wasPrimary = image.isPrimary();
        auctionImageRepository.delete(image);

        if (wasPrimary) {
            auctionImageRepository.findByAuctionOrderByDisplayOrderAsc(auction).stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setPrimary(true);
                        auctionImageRepository.save(next);
                    });
        }
    }

    @Override
    @Transactional
    public AuctionImageResponse setPrimary(User owner, Long auctionId, Long imageId) {
        Auction auction = findOwnedDraftOrThrow(owner, auctionId);
        AuctionImage image = auctionImageRepository.findByIdAndAuction(imageId, auction)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        auctionImageRepository.clearPrimaryForAuction(auction);
        image.setPrimary(true);
        return toResponse(auctionImageRepository.save(image));
    }

    private Auction findOwnedDraftOrThrow(User owner, Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (!auction.getSeller().getId().equals(owner.getId())) {
            throw new OwnershipException("You do not have permission to modify this auction's images");
        }
        if (auction.getStatus() != AuctionStatus.DRAFT) {
            throw new InvalidStateTransitionException("Images can only be modified while the auction is DRAFT");
        }
        return auction;
    }

    private AuctionImageResponse toResponse(AuctionImage image) {
        return AuctionImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isPrimary(image.isPrimary())
                .displayOrder(image.getDisplayOrder())
                .build();
    }
}
