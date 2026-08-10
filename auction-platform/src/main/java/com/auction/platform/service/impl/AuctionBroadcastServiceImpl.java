package com.auction.platform.service.impl;

import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.entity.Auction;
import com.auction.platform.mapper.AuctionMapper;
import com.auction.platform.service.AuctionBroadcastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionBroadcastServiceImpl implements AuctionBroadcastService {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuctionMapper auctionMapper;

    @Override
    public void broadcastAfterCommit(Auction auction) {
        AuctionResponse publicPayload = auctionMapper.toResponse(auction, false);
        Long auctionId = auction.getId();

        Runnable doBroadcast = () -> {
            try {
                messagingTemplate.convertAndSend("/topic/auctions/" + auctionId, publicPayload);
            } catch (Exception e) {
                // A broadcast failure should never fail the underlying business operation —
                // the bid itself already succeeded and was persisted; this is a best-effort push.
                log.warn("Failed to broadcast auction update for auction {}: {}", auctionId, e.getMessage());
            }
        };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doBroadcast.run();
                }
            });
        } else {
            doBroadcast.run();
        }
    }
}
