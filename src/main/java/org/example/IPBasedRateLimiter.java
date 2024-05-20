package org.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class IPBasedRateLimiter {
    private final Map<String, RateLimiter> ipRateLimiters = new ConcurrentHashMap<>();
    private long permitsPerSecond;

    public IPBasedRateLimiter(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
    }

    private RateLimiter getRateLimiterForIP(String ip) {
        return ipRateLimiters.computeIfAbsent(ip, k -> new FixedWindowRateLimiter(permitsPerSecond));
    }

    public boolean acquire(String ip) {
        RateLimiter rateLimiter = getRateLimiterForIP(ip);
        return rateLimiter.acquire();
    }

    public boolean tryAcquire(String ip, long timeout, TimeUnit unit) {
        RateLimiter rateLimiter = getRateLimiterForIP(ip);
        return rateLimiter.tryAcquire(timeout, unit);
    }

    public void setRate(String ip, double permitsPerSecond) {
        RateLimiter rateLimiter = getRateLimiterForIP(ip);
        rateLimiter.setRate(permitsPerSecond);
    }

    public double getRate(String ip) {
        RateLimiter rateLimiter = getRateLimiterForIP(ip);
        return rateLimiter.getRate();
    }

}