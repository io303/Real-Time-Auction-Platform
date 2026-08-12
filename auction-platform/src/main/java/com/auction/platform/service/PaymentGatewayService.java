package com.auction.platform.service;

import java.math.BigDecimal;

public interface PaymentGatewayService {

    record GatewayOrder(String orderId) {}
    record GatewayRefundResult(String refundId, boolean success) {}

    GatewayOrder createOrder(BigDecimal amount, String currency, String receiptId);

    /** Verifies the webhook payload actually came from the gateway (HMAC signature check). */
    boolean verifyWebhookSignature(String rawPayload, String signatureHeader);

    GatewayRefundResult refund(String gatewayTransactionId, BigDecimal amount);
}
