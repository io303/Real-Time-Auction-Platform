package com.auction.platform.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuctionImageResponse {
    private Long id;
    private String imageUrl;
    private boolean isPrimary;
    private int displayOrder;
}
