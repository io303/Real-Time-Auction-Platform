package com.auction.platform.service;

import com.auction.platform.dto.response.PaymentResponse;
import com.auction.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

public interface PaymentService {

    PaymentResponse initiatePayment(User buyer, Long auctionId);

    void handleWebhook(String rawPayload, String signatureHeader);

    PaymentResponse refund(User requester, Long paymentId);

    Page<PaymentResponse> getMyPaymentHistory(
            User buyer,
            Pageable pageable
    );

    PaymentResponse getById(
            User requester,
            Long paymentId
    );

    PaymentResponse simulateSuccess(User buyer, Long paymentId);

    /*
     * DEVELOPMENT ONLY
     *
     * Simulates a successful payment because the current project
     * uses MockPaymentGatewayServiceImpl instead of Razorpay/Stripe.
     *
     * Remove this endpoint when integrating a real payment gateway.
     */
    @Transactional
    PaymentResponse mockSuccess(User buyer, Long paymentId);

    // Development/testing only
    PaymentResponse simulateSuccessfulPayment(
            User buyer,
            Long paymentId
    );
    PaymentResponse verifyPayment(
            User buyer,
            String orderId,
            String paymentId,
            String signature
    );
}