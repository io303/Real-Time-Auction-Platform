package com.auction.platform.dto.response;

import com.auction.platform.entity.enums.AuctionStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuctionResponse {

    private Long id;
    private String title;
    private String description;
    private CategoryResponse category;
    private String sellerName;

    private BigDecimal startingPrice;

    private BigDecimal reservePrice;
    private Boolean reserveMet;

    private BigDecimal minIncrement;
    private BigDecimal currentHighestBid;

    private Long currentHighestBidderId;
    private String currentHighestBidderName;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private AuctionStatus status;

    private List<AuctionImageResponse> images;
}