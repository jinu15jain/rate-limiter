package org.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class FixedWindowRateLimiter implements RateLimiter {
    private final ReentrantLock lock = new ReentrantLock();

    private long currentWindowStart;
    private final long windowSizeInNanos;

    private long maxPermits;
    private long permitsUsed;
    private double permitsPerSecond;

    public FixedWindowRateLimiter(long permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
        this.maxPermits = permitsPerSecond;
        this.windowSizeInNanos = 1_000_000_000; // 1 second in nanoseconds
        this.currentWindowStart = System.nanoTime();
        this.permitsUsed = 0;
    }

    @Override
    public boolean acquire() {
        lock.lock();
        try {
            long now = System.nanoTime();
            if (now - currentWindowStart >= windowSizeInNanos) {
                currentWindowStart = now;
                permitsUsed = 0;
            }
            if (permitsUsed < maxPermits) {
                permitsUsed++;
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
                long now = System.nanoTime();
                if (now - currentWindowStart >= windowSizeInNanos) {
                    currentWindowStart = now;
                    permitsUsed = 0;
                }
                if (permitsUsed < maxPermits) {
                    permitsUsed++;
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
            this.permitsPerSecond = permitsPerSecond;
            this.maxPermits = (long) permitsPerSecond;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public double getRate() {
        lock.lock();
        try {
            return permitsPerSecond;
        } finally {
            lock.unlock();
        }
    }
}
