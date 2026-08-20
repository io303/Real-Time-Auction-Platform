package com.auction.platform.service.impl;

import com.auction.platform.service.PaymentGatewayService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Slf4j
public class PaymentGatewayServiceImpl
        implements PaymentGatewayService {

    private final RazorpayClient razorpayClient;

    private final String keySecret;
    private final String webhookSecret;

    public PaymentGatewayServiceImpl(
            @Value("${razorpay.key-id}") String keyId,
            @Value("${razorpay.key-secret}") String keySecret,
            @Value("${razorpay.webhook-secret}") String webhookSecret
    ) throws Exception {

        this.razorpayClient =
                new RazorpayClient(keyId, keySecret);

        this.keySecret = keySecret;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public GatewayOrder createOrder(
            BigDecimal amount,
            String currency,
            String receipt
    ) {

        try {

            long amountInPaise =
                    amount
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(0, RoundingMode.HALF_UP)
                            .longValueExact();

            JSONObject options = new JSONObject();

            options.put("amount", amountInPaise);
            options.put("currency", currency);
            options.put("receipt", receipt);
            options.put("payment_capture", 1);

            Order order =
                    razorpayClient.orders.create(options);

            String orderId =
                    order.get("id");

            log.info(
                    "Razorpay order created: {}",
                    orderId
            );

            return new GatewayOrder(orderId);

        } catch (Exception e) {

            log.error(
                    "Failed to create Razorpay order",
                    e
            );

            throw new RuntimeException(
                    "Unable to create Razorpay order",
                    e
            );
        }
    }

    @Override
    public boolean verifyPaymentSignature(
            String orderId,
            String paymentId,
            String signature
    ) {

        try {

            JSONObject options =
                    new JSONObject();

            options.put(
                    "razorpay_order_id",
                    orderId
            );

            options.put(
                    "razorpay_payment_id",
                    paymentId
            );

            options.put(
                    "razorpay_signature",
                    signature
            );

            return Utils.verifyPaymentSignature(
                    options,
                    keySecret
            );

        } catch (Exception e) {

            log.error(
                    "Razorpay payment signature verification failed",
                    e
            );

            return false;
        }
    }

    @Override
    public boolean verifyWebhookSignature(
            String rawPayload,
            String signature
    ) {

        try {

            if (signature == null ||
                    signature.isBlank()) {
                return false;
            }

            return Utils.verifyWebhookSignature(
                    rawPayload,
                    signature,
                    webhookSecret
            );

        } catch (Exception e) {

            log.error(
                    "Razorpay webhook signature verification failed",
                    e
            );

            return false;
        }
    }

    @Override
    public void refund(
            String transactionId,
            BigDecimal amount
    ) {

        try {

            long amountInPaise =
                    amount
                            .multiply(BigDecimal.valueOf(100))
                            .setScale(0, RoundingMode.HALF_UP)
                            .longValueExact();

            JSONObject options =
                    new JSONObject();

            options.put(
                    "amount",
                    amountInPaise
            );

            razorpayClient.payments
                    .refund(transactionId, options);

        } catch (Exception e) {

            log.error(
                    "Razorpay refund failed",
                    e
            );

            throw new RuntimeException(
                    "Unable to process refund",
                    e
            );
        }
    }
}