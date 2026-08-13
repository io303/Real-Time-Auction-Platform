package com.auction.platform.controller;

import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.AuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/auctions")
@RequiredArgsConstructor
@Tag(name = "Admin - Auctions", description = "Admin-only auction removal")
public class AdminAuctionController {

    private final AuctionService auctionService;

    @DeleteMapping("/{auctionId}")
    @Operation(summary = "Remove an auction (sets status to CANCELLED — history is preserved, not hard-deleted)")
    public ResponseEntity<ApiResponse<AuctionResponse>> remove(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long auctionId) {
        return ResponseEntity.ok(ApiResponse.success("Auction removed",
                auctionService.adminCancel(principal.getUser(), auctionId)));
    }
}
