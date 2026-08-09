package com.auction.platform.controller;

import com.auction.platform.dto.request.SetAutoBidRequest;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.dto.response.AutoBidResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.AutoBidService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/auto-bid")
@RequiredArgsConstructor
@Tag(name = "Auto-Bid", description = "Proxy bidding — set a max bid, system auto-raises for you")
public class AutoBidController {

    private final AutoBidService autoBidService;

    @PostMapping
    @Operation(summary = "Set or raise your max bid for this auction")
    public ResponseEntity<ApiResponse<AuctionResponse>> setAutoBid(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId,
            @Valid @RequestBody SetAutoBidRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Auto-bid set",
                autoBidService.setAutoBid(principal.getUser(), auctionId, request)));
    }

    @DeleteMapping
    @Operation(summary = "Cancel your auto-bid (stops future auto-raises)")
    public ResponseEntity<ApiResponse<MessageResponse>> cancel(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId) {
        autoBidService.cancelAutoBid(principal.getUser(), auctionId);
        return ResponseEntity.ok(ApiResponse.success("Auto-bid cancelled", MessageResponse.of("Auto-bid cancelled")));
    }

    @GetMapping
    @Operation(summary = "Get your own auto-bid setting for this auction (private — never visible to others)")
    public ResponseEntity<ApiResponse<AutoBidResponse>> getMine(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Auto-bid fetched",
                autoBidService.getMyAutoBid(principal.getUser(), auctionId)));
    }
}
