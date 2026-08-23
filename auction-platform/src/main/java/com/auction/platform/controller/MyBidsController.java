package com.auction.platform.controller;

import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.BidResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/bids")
@RequiredArgsConstructor
@Tag(name = "My Bids", description = "Authenticated user's bid history")
public class MyBidsController {

    private final BidService bidService;

    @GetMapping("/my")
    @Operation(
            summary = "Get my bids",
            description = "Returns paginated bid history of the authenticated user"
    )
    public ResponseEntity<ApiResponse<Page<BidResponse>>> getMyBids(
            @AuthenticationPrincipal CustomUserDetails principal,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "My bids fetched successfully",
                        bidService.getMyBids(
                                principal.getUser(),
                                pageable
                        )
                )
        );
    }
}