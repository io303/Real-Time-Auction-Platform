package com.auction.platform.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentWebhookRequest {
    private String gatewayOrderId;
    private String gatewayTransactionId;
    private String status; // "SUCCESS" | "FAILED" — as reported by the gateway
}
