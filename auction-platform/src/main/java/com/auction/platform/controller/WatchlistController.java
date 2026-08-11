package com.auction.platform.controller;

import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.WatchlistService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/watchlist")
@RequiredArgsConstructor
@Tag(name = "Watchlist", description = "Track auctions you're interested in")
public class WatchlistController {

    private final WatchlistService watchlistService;

    @PostMapping("/{auctionId}")
    @Operation(summary = "Add an auction to your watchlist")
    public ResponseEntity<ApiResponse<MessageResponse>> add(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long auctionId) {
        watchlistService.addToWatchlist(principal.getUser(), auctionId);
        return ResponseEntity.ok(ApiResponse.success("Added to watchlist", MessageResponse.of("Watching this auction")));
    }

    @DeleteMapping("/{auctionId}")
    @Operation(summary = "Remove an auction from your watchlist")
    public ResponseEntity<ApiResponse<MessageResponse>> remove(
            @AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long auctionId) {
        watchlistService.removeFromWatchlist(principal.getUser(), auctionId);
        return ResponseEntity.ok(ApiResponse.success("Removed from watchlist", MessageResponse.of("No longer watching this auction")));
    }

    @GetMapping
    @Operation(summary = "List your watchlist")
    public ResponseEntity<ApiResponse<List<AuctionResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Watchlist fetched", watchlistService.getMyWatchlist(principal.getUser())));
    }
}
