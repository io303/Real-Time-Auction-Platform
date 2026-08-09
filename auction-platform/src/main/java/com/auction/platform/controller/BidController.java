package com.auction.platform.controller;

import com.auction.platform.dto.request.PlaceBidRequest;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.dto.response.BidResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.BidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions/{auctionId}")
@RequiredArgsConstructor
@Tag(name = "Bids", description = "Bid placement and history")
public class BidController {

    private final BidService bidService;

    @PostMapping("/bids")
    @Operation(summary = "Place a bid (auction must be LIVE)")
    public ResponseEntity<ApiResponse<AuctionResponse>> placeBid(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId,
            @Valid @RequestBody PlaceBidRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Bid placed",
                bidService.placeBid(principal.getUser(), auctionId, request)));
    }

    @GetMapping("/bids")
    @Operation(summary = "Get bid history for an auction (public, paginated)")
    public ResponseEntity<ApiResponse<Page<BidResponse>>> getBidHistory(
            @PathVariable Long auctionId, Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Bid history fetched",
                bidService.getBidHistory(auctionId, pageable)));
    }

    @PostMapping("/force-live")
    @Operation(summary = "[TEMPORARY — removed once Phase 9 scheduler exists] Manually mark a SCHEDULED auction as LIVE")
    public ResponseEntity<ApiResponse<AuctionResponse>> forceLive(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Auction is now live",
                bidService.forceLive(principal.getUser(), auctionId)));
    }
}
