package com.auction.platform.controller;

import com.auction.platform.dto.request.PaymentVerificationRequest;
import com.auction.platform.dto.response.ApiResponse;
import com.auction.platform.dto.response.PaymentResponse;
import com.auction.platform.security.userdetails.CustomUserDetails;
import com.auction.platform.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Tag(
        name = "Payments",
        description = "Payment initiation, webhook confirmation, refunds, history"
)
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/auctions/{auctionId}/initiate")
    @Operation(summary = "Initiate payment for a won auction (winner only)")
    public ResponseEntity<ApiResponse<PaymentResponse>> initiate(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long auctionId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment initiated",
                        paymentService.initiatePayment(
                                principal.getUser(),
                                auctionId
                        )
                )
        );
    }


    @PostMapping("/verify")
    @Operation(
            summary = "Verify Razorpay payment"
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody PaymentVerificationRequest request
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment verified successfully",
                        paymentService.verifyPayment(
                                principal.getUser(),
                                request.getRazorpayOrderId(),
                                request.getRazorpayPaymentId(),
                                request.getRazorpaySignature()
                        )
                )
        );
    }

    @PostMapping("/webhook")
    @Operation(
            summary = "Gateway webhook (server-to-server, signature-verified — not for direct client use)"
    )
    public ResponseEntity<Void> webhook(
            HttpServletRequest request,
            @RequestHeader(
                    value = "X-Gateway-Signature",
                    required = false
            ) String signature
    ) throws IOException {

        String rawPayload = new String(
                request.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8
        );

        paymentService.handleWebhook(
                rawPayload,
                signature
        );

        return ResponseEntity.ok().build();
    }

    /*
     * ---------------------------------------------------------
     * DEVELOPMENT ONLY
     * ---------------------------------------------------------
     */

    @PostMapping("/{paymentId}/simulate-success")
    @Operation(
            summary = "DEV ONLY - Simulate successful payment"
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> simulateSuccess(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long paymentId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment simulated successfully",
                        paymentService.simulateSuccessfulPayment(
                                principal.getUser(),
                                paymentId
                        )
                )
        );
    }

    @PostMapping("/{paymentId}/refund")
    @Operation(
            summary = "Refund a successful payment (seller or admin only)"
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long paymentId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment refunded",
                        paymentService.refund(
                                principal.getUser(),
                                paymentId
                        )
                )
        );
    }

    @GetMapping("/my-history")
    @Operation(
            summary = "Your payment history, paginated"
    )
    public ResponseEntity<ApiResponse<Page<PaymentResponse>>> myHistory(
            @AuthenticationPrincipal CustomUserDetails principal,
            Pageable pageable
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment history fetched",
                        paymentService.getMyPaymentHistory(
                                principal.getUser(),
                                pageable
                        )
                )
        );
    }


    @GetMapping("/{paymentId}")
    @Operation(
            summary = "Get a single payment (buyer, seller, or admin only)"
    )
    public ResponseEntity<ApiResponse<PaymentResponse>> getById(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long paymentId
    ) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Payment fetched",
                        paymentService.getById(
                                principal.getUser(),
                                paymentId
                        )
                )
        );
    }
}