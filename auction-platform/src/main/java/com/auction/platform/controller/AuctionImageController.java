package com.auction.platform.controller;

import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.AuctionImageResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.AuctionImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auctions/{auctionId}/images")
@RequiredArgsConstructor
@Tag(name = "Auction Images", description = "Manage images on DRAFT auctions (owner only)")
public class AuctionImageController {

    private final AuctionImageService auctionImageService;

    @PostMapping
    @Operation(summary = "Add an image (max 8 per auction, DRAFT only)")
    public ResponseEntity<ApiResponse<List<AuctionImageResponse>>> add(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Image added",
                auctionImageService.addImage(principal.getUser(), auctionId, file)));
    }

    @DeleteMapping("/{imageId}")
    @Operation(summary = "Delete an image")
    public ResponseEntity<ApiResponse<MessageResponse>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId,
            @PathVariable Long imageId) {
        auctionImageService.deleteImage(principal.getUser(), auctionId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted", MessageResponse.of("Image removed")));
    }

    @PatchMapping("/{imageId}/primary")
    @Operation(summary = "Set an image as primary")
    public ResponseEntity<ApiResponse<AuctionImageResponse>> setPrimary(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId,
            @PathVariable Long imageId) {
        return ResponseEntity.ok(ApiResponse.success("Primary image set",
                auctionImageService.setPrimary(principal.getUser(), auctionId, imageId)));
    }
}
