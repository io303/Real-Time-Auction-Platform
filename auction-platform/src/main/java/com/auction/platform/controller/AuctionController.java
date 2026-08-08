package com.auction.platform.controller;

import com.auction.platform.dto.request.CreateAuctionRequest;
import com.auction.platform.dto.request.UpdateAuctionRequest;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.AuctionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auctions")
@RequiredArgsConstructor
@Tag(name = "Auctions", description = "Auction listing CRUD (Draft/Scheduled)")
public class AuctionController {

    private final AuctionService auctionService;

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @Operation(summary = "Create a new auction as DRAFT (seller only)")
    public ResponseEntity<ApiResponse<AuctionResponse>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody CreateAuctionRequest request) {
        AuctionResponse response = auctionService.create(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Auction created as draft", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a DRAFT auction (owner only)")
    public ResponseEntity<ApiResponse<AuctionResponse>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAuctionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Auction updated",
                auctionService.update(principal.getUser(), id, request)));
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish a DRAFT auction (DRAFT -> SCHEDULED, owner only)")
    public ResponseEntity<ApiResponse<AuctionResponse>> publish(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Auction published",
                auctionService.publish(principal.getUser(), id)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an auction (DRAFT or SCHEDULED only, owner only)")
    public ResponseEntity<ApiResponse<MessageResponse>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        auctionService.delete(principal.getUser(), id);
        return ResponseEntity.ok(ApiResponse.success("Auction deleted", MessageResponse.of("Auction removed")));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an auction by id (public if published, owner/admin can view drafts)")
    public ResponseEntity<ApiResponse<AuctionResponse>> getById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long id) {
        var requester = principal != null ? principal.getUser() : null;
        return ResponseEntity.ok(ApiResponse.success("Auction fetched", auctionService.getById(requester, id)));
    }

    @GetMapping
    @Operation(summary = "Browse publicly visible auctions (paginated)")
    public ResponseEntity<ApiResponse<Page<AuctionResponse>>> listPublished(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Auctions fetched", auctionService.listPublished(pageable)));
    }

    @GetMapping("/mine")
    @Operation(summary = "List the authenticated seller's own auctions, all statuses")
    public ResponseEntity<ApiResponse<List<AuctionResponse>>> listMine(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Your auctions fetched",
                auctionService.listMine(principal.getUser())));
    }
}
