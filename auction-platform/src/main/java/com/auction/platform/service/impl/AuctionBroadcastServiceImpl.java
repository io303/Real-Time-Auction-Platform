package com.auction.platform.service.impl;

import com.auction.platform.config.RedisConfig;
import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.mapper.AuctionMapper;
import com.auction.platform.service.AuctionBroadcastService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionBroadcastServiceImpl implements AuctionBroadcastService {

    private final RedisTemplate<String, String> redisTemplate;
    private final AuctionMapper auctionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void broadcastAfterCommit(Auction auction) {
        AuctionResponse publicPayload = auctionMapper.toResponse(auction, false);
        Long auctionId = auction.getId();

        Runnable doPublish = () -> {
            try {
                // Payload wraps auctionId + the response together, since Redis pub/sub is a
                // single flat channel — the subscriber needs to know which STOMP topic to
                // re-publish to locally.
                String message = objectMapper.writeValueAsString(
                        new AuctionUpdateMessage(auctionId, publicPayload));
                redisTemplate.convertAndSend(RedisConfig.AUCTION_UPDATES_CHANNEL, message);
            } catch (Exception e) {
                log.warn("Failed to publish auction update to Redis for auction {}: {}", auctionId, e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish.run();
                }
            });
        } else {
            doPublish.run();
        }
    }

    public record AuctionUpdateMessage(Long auctionId, AuctionResponse payload) {}
}
