package com.auction.platform.service;

public interface RateLimiterService {
    /** Returns true if the request is allowed, false if the rate limit was exceeded. */
    boolean isAllowed(String key, int maxRequests, int windowSeconds);
}
