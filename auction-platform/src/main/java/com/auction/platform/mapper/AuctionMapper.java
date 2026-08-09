package com.auction.platform.mapper;

import com.auction.platform.dto.response.AuctionImageResponse;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.AuctionImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuctionMapper {

    private final CategoryMapper categoryMapper;

    /** includeReservePrice should be true only when the caller is the owner or an admin. */
    public AuctionResponse toResponse(Auction auction, boolean includeReservePrice) {
        Boolean reserveMet = auction.getReservePrice() == null
                ? null
                : auction.getCurrentHighestBid() != null
                    && auction.getCurrentHighestBid().compareTo(auction.getReservePrice()) >= 0;

        List<AuctionImageResponse> images = auction.getImages() == null ? List.of() :
                auction.getImages().stream()
                        .sorted(Comparator.comparingInt(AuctionImage::getDisplayOrder))
                        .map(img -> AuctionImageResponse.builder()
                                .id(img.getId())
                                .imageUrl(img.getImageUrl())
                                .isPrimary(img.isPrimary())
                                .displayOrder(img.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList());

        return AuctionResponse.builder()
                .id(auction.getId())
                .title(auction.getTitle())
                .description(auction.getDescription())
                .category(categoryMapper.toResponse(auction.getCategory()))
                .sellerName(auction.getSeller().getFullName())
                .startingPrice(auction.getStartingPrice())
                .reservePrice(includeReservePrice ? auction.getReservePrice() : null)
                .reserveMet(reserveMet)
                .minIncrement(auction.getMinIncrement())
                .currentHighestBid(auction.getCurrentHighestBid())
                .startDate(auction.getStartDate())
                .endDate(auction.getEndDate())
                .status(auction.getStatus())
                .images(images)
                .build();
    }
}
