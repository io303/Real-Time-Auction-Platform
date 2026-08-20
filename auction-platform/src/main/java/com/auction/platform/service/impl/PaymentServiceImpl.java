package com.auction.platform.service.impl;

import com.auction.platform.dto.request.PaymentWebhookRequest;
import com.auction.platform.dto.response.PaymentResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.entity.Payment;
import com.auction.platform.entity.User;
import com.auction.platform.entity.enums.AuctionStatus;
import com.auction.platform.entity.enums.NotificationType;
import com.auction.platform.entity.enums.PaymentStatus;
import com.auction.platform.exception.ApiException;
import com.auction.platform.exception.InvalidWebhookSignatureException;
import com.auction.platform.exception.OwnershipException;
import com.auction.platform.exception.PaymentNotAllowedException;
import com.auction.platform.exception.ResourceNotFoundException;
import com.auction.platform.repository.AuctionRepository;
import com.auction.platform.repository.PaymentRepository;
import com.auction.platform.service.NotificationService;
import com.auction.platform.service.PaymentGatewayService;
import com.auction.platform.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;


@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final AuctionRepository auctionRepository;
    private final PaymentGatewayService gatewayService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(User buyer, Long auctionId) {

        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Auction not found"));

        if (auction.getStatus() != AuctionStatus.ENDED) {
            throw new PaymentNotAllowedException(
                    "Payment can only be made for an ended auction"
            );
        }

        if (auction.getCurrentHighestBidder() == null ||
                !auction.getCurrentHighestBidder()
                        .getId()
                        .equals(buyer.getId())) {

            throw new PaymentNotAllowedException(
                    "Only the winning bidder can pay for this auction"
            );
        }

        if (auction.getReservePrice() != null &&
                auction.getCurrentHighestBid()
                        .compareTo(auction.getReservePrice()) < 0) {

            throw new PaymentNotAllowedException(
                    "Reserve price was not met — there is no valid winner to pay"
            );
        }

        boolean hasActivePayment = !paymentRepository
                .findByAuctionAndStatusIn(
                        auction,
                        List.of(
                                PaymentStatus.PENDING,
                                PaymentStatus.SUCCESS
                        )
                )
                .isEmpty();

        if (hasActivePayment) {
            throw new PaymentNotAllowedException(
                    "A payment for this auction is already pending or completed"
            );
        }

        BigDecimal amount = auction.getCurrentHighestBid();

        PaymentGatewayService.GatewayOrder order =
                gatewayService.createOrder(
                        amount,
                        "INR",
                        "auction-" + auctionId
                );

        Payment payment = Payment.builder()
                .auction(auction)
                .buyer(buyer)
                .seller(auction.getSeller())
                .amount(amount)
                .status(PaymentStatus.PENDING)
                .gatewayOrderId(order.orderId())
                .build();

        Payment saved = paymentRepository.save(payment);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(
            User buyer,
            String orderId,
            String paymentId,
            String signature
    ) {

        Payment payment =
                paymentRepository
                        .findByGatewayOrderId(orderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found"
                                )
                        );

        // Ensure this payment belongs to the logged-in buyer
        if (!payment.getBuyer()
                .getId()
                .equals(buyer.getId())) {

            throw new OwnershipException(
                    "You cannot verify this payment"
            );
        }

        // Already successful
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentNotAllowedException(
                    "Payment cannot be verified. Current status: "
                            + payment.getStatus()
            );
        }

        boolean valid =
                gatewayService.verifyPaymentSignature(
                        orderId,
                        paymentId,
                        signature
                );

        if (!valid) {

            throw new InvalidWebhookSignatureException(
                    "Invalid Razorpay payment signature"
            );
        }

        payment.setGatewayTransactionId(paymentId);
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment saved =
                paymentRepository.save(payment);

        notificationService.notifyUser(
                payment.getBuyer(),
                NotificationType.PAYMENT_SUCCESSFUL,
                "Payment successful: "
                        + payment.getAuction().getTitle(),
                "Your payment of "
                        + payment.getAmount()
                        + " "
                        + payment.getCurrency()
                        + " was successful.",
                payment.getAuction()
        );

        return toResponse(saved);
    }

    /*
     * DEVELOPMENT ONLY
     *
     * Simulates a successful payment because the current project
     * uses MockPaymentGatewayServiceImpl instead of Razorpay/Stripe.
     *
     * Remove this endpoint when integrating a real payment gateway.
     */
    @Transactional
    @Override
    public PaymentResponse mockSuccess(User buyer, Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Payment not found"));

        // Only the winning buyer can complete this payment
        if (!payment.getBuyer().getId().equals(buyer.getId())) {
            throw new PaymentNotAllowedException(
                    "You are not allowed to complete this payment"
            );
        }

        // Payment must still be pending
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentNotAllowedException(
                    "Payment is not pending. Current status: "
                            + payment.getStatus()
            );
        }

        // Generate fake gateway transaction ID
        payment.setGatewayTransactionId(
                "mock_txn_" + UUID.randomUUID()
        );

        // Mark payment successful
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment saved = paymentRepository.save(payment);

        // Notify buyer
        notificationService.notifyUser(
                payment.getBuyer(),
                NotificationType.PAYMENT_SUCCESSFUL,
                "Payment successful: "
                        + payment.getAuction().getTitle(),
                "Your payment of "
                        + payment.getAmount()
                        + " "
                        + payment.getCurrency()
                        + " was successful.",
                payment.getAuction()
        );

        log.info(
                "[MOCK PAYMENT] Payment {} marked SUCCESS for auction {}",
                payment.getId(),
                payment.getAuction().getId()
        );

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse simulateSuccessfulPayment(User buyer, Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Only the buyer can simulate his own payment
        if (!payment.getBuyer().getId().equals(buyer.getId())) {
            throw new OwnershipException("You cannot update this payment");
        }

        // Payment must be pending
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentNotAllowedException(
                    "Only a pending payment can be completed. Current status: "
                            + payment.getStatus()
            );
        }

        // Generate fake transaction ID
        String transactionId =
                "mock_txn_" + java.util.UUID.randomUUID();

        payment.setGatewayTransactionId(transactionId);
        payment.setStatus(PaymentStatus.SUCCESS);

        Payment saved = paymentRepository.save(payment);

        // Send notification
        notificationService.notifyUser(
                payment.getBuyer(),
                NotificationType.PAYMENT_SUCCESSFUL,
                "Payment successful: " + payment.getAuction().getTitle(),
                "Your payment of "
                        + payment.getAmount()
                        + " "
                        + payment.getCurrency()
                        + " was successful.",
                payment.getAuction()
        );

        log.info(
                "[MOCK PAYMENT] Payment {} marked SUCCESS for buyer {}",
                paymentId,
                buyer.getId()
        );

        return toResponse(saved);
    }


    @Override
    @Transactional
    public void handleWebhook(
            String rawPayload,
            String signatureHeader) {

        if (!gatewayService.verifyWebhookSignature(
                rawPayload,
                signatureHeader)) {

            throw new InvalidWebhookSignatureException(
                    "Payment webhook signature verification failed"
            );
        }

        PaymentWebhookRequest webhookData;

        try {
            webhookData = objectMapper.readValue(
                    rawPayload,
                    PaymentWebhookRequest.class
            );
        } catch (Exception e) {

            throw new ApiException(
                    "Malformed webhook payload",
                    HttpStatus.BAD_REQUEST
            );
        }

        Payment payment =
                paymentRepository.findByGatewayOrderId(
                                webhookData.getGatewayOrderId()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found for order "
                                                + webhookData.getGatewayOrderId()
                                )
                        );

        /*
         * Idempotency:
         * If gateway sends the same webhook multiple times,
         * don't process it again.
         */
        if (payment.getStatus() != PaymentStatus.PENDING) {

            log.info(
                    "Webhook received for payment {} already in status {} — ignoring duplicate delivery",
                    payment.getId(),
                    payment.getStatus()
            );

            return;
        }

        payment.setGatewayTransactionId(
                webhookData.getGatewayTransactionId()
        );

        if ("SUCCESS".equalsIgnoreCase(
                webhookData.getStatus())) {

            payment.setStatus(PaymentStatus.SUCCESS);

            paymentRepository.save(payment);

            notificationService.notifyUser(
                    payment.getBuyer(),
                    NotificationType.PAYMENT_SUCCESSFUL,
                    "Payment successful: "
                            + payment.getAuction().getTitle(),
                    "Your payment of "
                            + payment.getAmount()
                            + " "
                            + payment.getCurrency()
                            + " was successful.",
                    payment.getAuction()
            );

        } else {

            payment.setStatus(PaymentStatus.FAILED);

            paymentRepository.save(payment);

            log.info(
                    "Payment {} marked FAILED via webhook",
                    payment.getId()
            );
        }
    }

    @Override
    @Transactional
    public PaymentResponse refund(
            User requester,
            Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found"
                        ));

        boolean isSeller =
                payment.getSeller()
                        .getId()
                        .equals(requester.getId());

        boolean isAdmin =
                requester.getRoles()
                        .stream()
                        .anyMatch(
                                r -> r.getName()
                                        .name()
                                        .equals("ROLE_ADMIN")
                        );

        if (!isSeller && !isAdmin) {

            throw new OwnershipException(
                    "Only the seller or an admin can refund this payment"
            );
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {

            throw new PaymentNotAllowedException(
                    "Only a successful payment can be refunded. Current status: "
                            + payment.getStatus()
            );
        }

        gatewayService.refund(
                payment.getGatewayTransactionId(),
                payment.getAmount()
        );

        payment.setStatus(PaymentStatus.REFUNDED);

        Payment saved =
                paymentRepository.save(payment);

        return toResponse(saved);
    }

    @Override
    public Page<PaymentResponse> getMyPaymentHistory(
            User buyer,
            Pageable pageable) {

        return paymentRepository
                .findByBuyerOrderByCreatedAtDesc(
                        buyer,
                        pageable
                )
                .map(this::toResponse);
    }

    @Override
    public PaymentResponse getById(
            User requester,
            Long paymentId) {

        Payment payment =
                paymentRepository.findById(paymentId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Payment not found"
                                ));

        boolean isBuyer =
                payment.getBuyer()
                        .getId()
                        .equals(requester.getId());

        boolean isSeller =
                payment.getSeller()
                        .getId()
                        .equals(requester.getId());

        boolean isAdmin =
                requester.getRoles()
                        .stream()
                        .anyMatch(
                                r -> r.getName()
                                        .name()
                                        .equals("ROLE_ADMIN")
                        );

        if (!isBuyer && !isSeller && !isAdmin) {

            throw new ResourceNotFoundException(
                    "Payment not found"
            );
        }

        return toResponse(payment);
    }


    @Override
    @Transactional
    public PaymentResponse simulateSuccess(User buyer, Long paymentId) {

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        // Only buyer can simulate their own payment
        if (!payment.getBuyer().getId().equals(buyer.getId())) {
            throw new ResourceNotFoundException("Payment not found");
        }

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentNotAllowedException(
                    "Only a pending payment can be marked as successful. Current status: "
                            + payment.getStatus()
            );
        }

        // Generate mock transaction ID
        payment.setGatewayTransactionId(
                "mock_txn_" + java.util.UUID.randomUUID()
        );

        payment.setStatus(PaymentStatus.SUCCESS);

        Payment saved = paymentRepository.save(payment);

        // Notify user
        notificationService.notifyUser(
                payment.getBuyer(),
                NotificationType.PAYMENT_SUCCESSFUL,
                "Payment successful: " + payment.getAuction().getTitle(),
                "Your payment of " + payment.getAmount() + " "
                        + payment.getCurrency() + " was successful.",
                payment.getAuction()
        );

        return toResponse(saved);
    }

    private PaymentResponse toResponse(Payment p) {

        return PaymentResponse.builder()
                .id(p.getId())
                .auctionId(p.getAuction().getId())
                .auctionTitle(p.getAuction().getTitle())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .gatewayOrderId(p.getGatewayOrderId())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}