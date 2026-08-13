package com.auction.platform.controller;

import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.PlatformStatsResponse;
import com.auction.platform.service.AdminStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@Tag(name = "Admin - Statistics", description = "Platform-wide statistics and reporting")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @GetMapping
    @Operation(summary = "Get platform-wide statistics (users, auctions, bids, revenue)")
    public ResponseEntity<ApiResponse<PlatformStatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.success("Stats fetched", adminStatsService.getPlatformStats()));
    }
}
