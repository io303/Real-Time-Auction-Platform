package com.auction.platform.service.impl;

import com.auction.platform.dto.response.AuctionResponse;
import com.auction.platform.service.AuctionCacheService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionCacheServiceImpl implements AuctionCacheService {

    private static final String KEY_PREFIX = "auction:public:";
    // Short TTL acts as a safety net — even if an eviction call is ever missed somewhere,
    // staleness self-heals within a minute rather than persisting indefinitely.
    private static final Duration TTL = Duration.ofMinutes(1);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<AuctionResponse> getCachedPublicView(Long auctionId) {
        try {
            String json = redisTemplate.opsForValue().get(key(auctionId));
            if (json == null) {
                return Optional.empty();
            }
            AuctionResponse cached = objectMapper.readValue(json, AuctionResponse.class);
            return Optional.of(cached);
        } catch (Exception e) {
            // Cache read failures degrade to "cache miss", never to an error — Redis being
            // temporarily unavailable must never break the auction-viewing flow.
            log.warn("Redis cache read failed for auction {}: {}", auctionId, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void putPublicView(Long auctionId, AuctionResponse publicResponse) {
        try {
            // Defensive guard: this cache must never persist a reserve price, regardless of
            // what the caller passes in.
            publicResponse.setReservePrice(null);
            String json = objectMapper.writeValueAsString(publicResponse);
            redisTemplate.opsForValue().set(key(auctionId), json, TTL);
        } catch (Exception e) {
            log.warn("Redis cache write failed for auction {}: {}", auctionId, e.getMessage());
        }
    }

    @Override
    public void evict(Long auctionId) {
        try {
            redisTemplate.delete(key(auctionId));
        } catch (Exception e) {
            log.warn("Redis cache evict failed for auction {}: {}", auctionId, e.getMessage());
        }
    }

    private String key(Long auctionId) {
        return KEY_PREFIX + auctionId;
    }
}
