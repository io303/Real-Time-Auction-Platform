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
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (auction.getStatus() != AuctionStatus.ENDED) {
            throw new PaymentNotAllowedException("Payment can only be made for an ended auction");
        }
        if (auction.getCurrentHighestBidder() == null || !auction.getCurrentHighestBidder().getId().equals(buyer.getId())) {
            throw new PaymentNotAllowedException("Only the winning bidder can pay for this auction");
        }
        if (auction.getReservePrice() != null && auction.getCurrentHighestBid().compareTo(auction.getReservePrice()) < 0) {
            throw new PaymentNotAllowedException("Reserve price was not met — there is no valid winner to pay");
        }

        boolean hasActivePayment = !paymentRepository
                .findByAuctionAndStatusIn(auction, List.of(PaymentStatus.PENDING, PaymentStatus.SUCCESS))
                .isEmpty();
        if (hasActivePayment) {
            throw new PaymentNotAllowedException("A payment for this auction is already pending or completed");
        }

        BigDecimal amount = auction.getCurrentHighestBid();
        PaymentGatewayService.GatewayOrder order = gatewayService.createOrder(amount, "INR", "auction-" + auctionId);

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
    public void handleWebhook(String rawPayload, String signatureHeader) {
        if (!gatewayService.verifyWebhookSignature(rawPayload, signatureHeader)) {
            throw new InvalidWebhookSignatureException("Webhook signature verification failed");
        }

        PaymentWebhookRequest webhookData;
        try {
            webhookData = objectMapper.readValue(rawPayload, PaymentWebhookRequest.class);
        } catch (Exception e) {
            throw new ApiException("Malformed webhook payload", HttpStatus.BAD_REQUEST);
        }

        Payment payment = paymentRepository.findByGatewayOrderId(webhookData.getGatewayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for order " + webhookData.getGatewayOrderId()));

        if (payment.getStatus() != PaymentStatus.PENDING) {
            log.info("Webhook received for payment {} already in status {} — ignoring duplicate delivery",
                    payment.getId(), payment.getStatus());
            return; // gateways often retry webhooks; this makes handling idempotent
        }

        payment.setGatewayTransactionId(webhookData.getGatewayTransactionId());

        if ("SUCCESS".equalsIgnoreCase(webhookData.getStatus())) {
            payment.setStatus(PaymentStatus.SUCCESS);
            paymentRepository.save(payment);

            notificationService.notifyUser(payment.getBuyer(), NotificationType.PAYMENT_SUCCESSFUL,
                    "Payment successful: " + payment.getAuction().getTitle(),
                    "Your payment of " + payment.getAmount() + " " + payment.getCurrency() + " was successful.",
                    payment.getAuction());
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            log.info("Payment {} marked FAILED via webhook", payment.getId());
        }
    }

    @Override
    @Transactional
    public PaymentResponse refund(User requester, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        boolean isSeller = payment.getSeller().getId().equals(requester.getId());
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
        if (!isSeller && !isAdmin) {
            throw new OwnershipException("Only the seller or an admin can refund this payment");
        }
        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentNotAllowedException("Only a successful payment can be refunded. Current status: " + payment.getStatus());
        }

        gatewayService.refund(payment.getGatewayTransactionId(), payment.getAmount());
        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);

        return toResponse(saved);
    }

    @Override
    public Page<PaymentResponse> getMyPaymentHistory(User buyer, Pageable pageable) {
        return paymentRepository.findByBuyerOrderByCreatedAtDesc(buyer, pageable).map(this::toResponse);
    }

    @Override
    public PaymentResponse getById(User requester, Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        boolean isBuyer = payment.getBuyer().getId().equals(requester.getId());
        boolean isSeller = payment.getSeller().getId().equals(requester.getId());
        boolean isAdmin = requester.getRoles().stream().anyMatch(r -> r.getName().name().equals("ROLE_ADMIN"));
        if (!isBuyer && !isSeller && !isAdmin) {
            throw new ResourceNotFoundException("Payment not found"); // don't reveal existence to unrelated parties
        }

        return toResponse(payment);
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
