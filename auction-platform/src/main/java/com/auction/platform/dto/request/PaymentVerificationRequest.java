package com.auction.platform.dto.request;

import lombok.Data;

@Data
public class PaymentVerificationRequest {

    private String razorpayOrderId;

    private String razorpayPaymentId;

    private String razorpaySignature;
}