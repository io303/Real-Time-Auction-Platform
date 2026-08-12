package com.auction.platform.service;

import com.auction.platform.dto.response.PaymentResponse;
import com.auction.platform.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PaymentService {
    PaymentResponse initiatePayment(User buyer, Long auctionId);
    void handleWebhook(String rawPayload, String signatureHeader);
    PaymentResponse refund(User requester, Long paymentId);
    Page<PaymentResponse> getMyPaymentHistory(User buyer, Pageable pageable);
    PaymentResponse getById(User requester, Long paymentId);
}
