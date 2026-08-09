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

    /** Only populated when the caller is the owner or an admin — never shown publicly. */
    private BigDecimal reservePrice;

    /** null if no reservePrice was set on this auction at all. */
    private Boolean reserveMet;

    private BigDecimal minIncrement;
    private BigDecimal currentHighestBid;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private AuctionStatus status;
    private List<AuctionImageResponse> images;
}
