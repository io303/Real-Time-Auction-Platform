package com.auction.platform.service.impl;

import com.auction.platform.service.RateLimiterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimiterServiceImpl implements RateLimiterService {

    private static final String KEY_PREFIX = "ratelimit:";

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean isAllowed(String key, int maxRequests, int windowSeconds) {
        try {
            String fullKey = KEY_PREFIX + key;
            Long count = redisTemplate.opsForValue().increment(fullKey);
            if (count != null && count == 1L) {
                // First request in this window — set expiry exactly once, so the window is
                // fixed from the first request, not sliding.
                redisTemplate.expire(fullKey, Duration.ofSeconds(windowSeconds));
            }
            return count == null || count <= maxRequests;
        } catch (Exception e) {
            // Fail OPEN, not closed: if Redis is down, we don't want to lock every user
            // out of login/bidding entirely — rate limiting degrades gracefully to "off".
            log.warn("Rate limiter check failed for key {}: {} — failing open", key, e.getMessage());
            return true;
        }
    }
}
