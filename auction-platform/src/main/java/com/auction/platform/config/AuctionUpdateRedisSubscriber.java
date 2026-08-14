package com.auction.platform.config;

import com.auction.platform.service.impl.AuctionBroadcastServiceImpl.AuctionUpdateMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionUpdateRedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            AuctionUpdateMessage update = objectMapper.readValue(message.getBody(), AuctionUpdateMessage.class);
            // This runs on EVERY instance that received the Redis pub/sub message — each one
            // delivers only to its own locally-connected WebSocket clients. Together, all
            // instances cover the full set of connected clients cluster-wide.
            messagingTemplate.convertAndSend("/topic/auctions/" + update.auctionId(), update.payload());
        } catch (Exception e) {
            log.warn("Failed to process Redis auction update message: {}", e.getMessage());
        }
    }
}
