package com.auction.platform.mapper;

import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.Auction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuctionMapper {

    private final CategoryMapper categoryMapper;

    /** includeReservePrice should be true only when the caller is the owner or an admin. */
    public AuctionResponse toResponse(Auction auction, boolean includeReservePrice) {
        return AuctionResponse.builder()
                .id(auction.getId())
                .title(auction.getTitle())
                .description(auction.getDescription())
                .category(categoryMapper.toResponse(auction.getCategory()))
                .sellerName(auction.getSeller().getFullName())
                .startingPrice(auction.getStartingPrice())
                .reservePrice(includeReservePrice ? auction.getReservePrice() : null)
                .minIncrement(auction.getMinIncrement())
                .startDate(auction.getStartDate())
                .endDate(auction.getEndDate())
                .status(auction.getStatus())
                .build();
    }
}
