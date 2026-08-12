package com.auction.platform.service.impl;

import com.auction.platform.service.PaymentGatewayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mock gateway — stands in for Razorpay/Stripe until real credentials exist (spec explicitly
 * defers this: "Assume Razorpay or Stripe integration later"). Swap this bean out for a real
 * RazorpayGatewayServiceImpl / StripeGatewayServiceImpl later — nothing else in the payment
 * flow needs to change, since everything talks to the PaymentGatewayService interface.
 */
@Service
@Slf4j
public class MockPaymentGatewayServiceImpl implements PaymentGatewayService {

    @Override
    public GatewayOrder createOrder(BigDecimal amount, String currency, String receiptId) {
        String orderId = "mock_order_" + UUID.randomUUID();
        log.info("[MOCK GATEWAY] Created order {} for {} {} (receipt: {})", orderId, amount, currency, receiptId);
        return new GatewayOrder(orderId);
    }

    @Override
    public boolean verifyWebhookSignature(String rawPayload, String signatureHeader) {
        // A real implementation computes HMAC-SHA256(rawPayload, webhookSecret) and compares
        // it to signatureHeader using a constant-time comparison. The mock always accepts,
        // since there's no real gateway signing anything yet — this is explicitly NOT
        // production-safe and is documented as such.
        log.warn("[MOCK GATEWAY] Signature verification is a no-op in mock mode — do not use in production");
        return true;
    }

    @Override
    public GatewayRefundResult refund(String gatewayTransactionId, BigDecimal amount) {
        String refundId = "mock_refund_" + UUID.randomUUID();
        log.info("[MOCK GATEWAY] Refunded {} for transaction {} -> refund {}", amount, gatewayTransactionId, refundId);
        return new GatewayRefundResult(refundId, true);
    }
}
