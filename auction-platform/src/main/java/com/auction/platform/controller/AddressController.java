package com.auction.platform.controller;

import com.auction.platform.dto.request.AddressRequest;
import com.auction.platform.dto.response.AddressResponse;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.MessageResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users/me/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Authenticated user's saved addresses")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @Operation(summary = "List all addresses, default first")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> list(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched",
                addressService.listAddresses(principal.getUser())));
    }

    @PostMapping
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<AddressResponse>> add(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.addAddress(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Address added", response));
    }

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an existing address")
    public ResponseEntity<ApiResponse<AddressResponse>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        AddressResponse response = addressService.updateAddress(principal.getUser(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated", response));
    }

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<MessageResponse>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long addressId) {
        addressService.deleteAddress(principal.getUser(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", MessageResponse.of("Address removed")));
    }

    @PatchMapping("/{addressId}/default")
    @Operation(summary = "Mark an address as the default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long addressId) {
        AddressResponse response = addressService.setDefaultAddress(principal.getUser(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Default address set", response));
    }
}
