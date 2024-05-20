package org.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class TokenBucketRateLimiter implements RateLimiter {
    private final long capacity;
    private double tokens;
    private final ReentrantLock lock = new ReentrantLock();
    private long refillIntervalInNanos;
    private long lastRefillTimestamp;
    private double refillTokens;

    public TokenBucketRateLimiter(long capacity, double permitsPerSecond) {
        this.capacity = capacity;
        this.refillTokens = permitsPerSecond;
        this.refillIntervalInNanos = (long) (1_000_000_000 / permitsPerSecond);
        this.tokens = capacity;
        this.lastRefillTimestamp = System.nanoTime();
    }

    @Override
    public boolean acquire() {
        lock.lock();
        try {
            refillTokensIfNeeded();
            if (tokens >= 1) {
                tokens--;
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public boolean tryAcquire(long timeout, TimeUnit unit) {
        long timeoutInNanos = unit.toNanos(timeout);
        long deadline = System.nanoTime() + timeoutInNanos;
        lock.lock();
        try {
            while (System.nanoTime() < deadline) {
                refillTokensIfNeeded();
                if (tokens >= 1) {
                    tokens--;
                    return true;
                }
                try {
                    TimeUnit.NANOSECONDS.sleep(1_000); // Sleep briefly to avoid busy waiting
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void setRate(double permitsPerSecond) {
        lock.lock();
        try {
            this.refillTokens = permitsPerSecond;
            this.refillIntervalInNanos = (long) (1_000_000_000 / permitsPerSecond);
            refillTokensIfNeeded();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public double getRate() {
        lock.lock();
        try {
            return refillTokens;
        } finally {
            lock.unlock();
        }
    }

    private void refillTokensIfNeeded() {
        long now = System.nanoTime();
        if (now > lastRefillTimestamp) {
            long elapsedTime = now - lastRefillTimestamp;
            double newTokens = (elapsedTime / (double) refillIntervalInNanos) * refillTokens;
            tokens = Math.min(capacity, tokens + newTokens);
            lastRefillTimestamp = now;
        }
    }
}
