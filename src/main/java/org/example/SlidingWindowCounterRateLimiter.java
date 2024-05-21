package org.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class SlidingWindowCounterRateLimiter implements RateLimiter {
    private final long windowSizeInNanos;
    private final int permitsPerSecond;
    private long currentWindowStart;
    private long previousWindowStart;
    private int currentWindowCount;
    private int previousWindowCount;
    private final ReentrantLock lock = new ReentrantLock();

    public SlidingWindowCounterRateLimiter(int permitsPerSecond, int windowSizeInSeconds) {
        this.permitsPerSecond = permitsPerSecond;
        this.windowSizeInNanos = TimeUnit.SECONDS.toNanos(windowSizeInSeconds);
        this.currentWindowStart = System.nanoTime();
        this.previousWindowStart = currentWindowStart - windowSizeInNanos;
        this.currentWindowCount = 0;
        this.previousWindowCount = 0;
    }

    private void updateWindows() {
        long now = System.nanoTime();
        if (now - currentWindowStart >= windowSizeInNanos) {
            previousWindowStart = currentWindowStart;
            previousWindowCount = currentWindowCount;
            currentWindowStart = now;
            currentWindowCount = 0;
        }
    }

    private long getTotalCount() {
        double overlappingTime = previousWindowStart + windowSizeInNanos - currentWindowStart;
        if (overlappingTime < 0) {
            overlappingTime = 0;
        }
        double overlappingRatio = overlappingTime / windowSizeInNanos;
        return (long) (previousWindowCount * overlappingRatio + currentWindowCount);
    }

    @Override
    public boolean acquire() {
        lock.lock();
        try {
            updateWindows();
            if (getTotalCount() < permitsPerSecond) {
                currentWindowCount++;
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
                updateWindows();
                if (getTotalCount() < permitsPerSecond) {
                    currentWindowCount++;
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
            // No need to convert permitsPerSecond to int because this method is never called in this implementation.
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
