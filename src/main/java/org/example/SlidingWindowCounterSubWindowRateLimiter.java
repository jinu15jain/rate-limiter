package org.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class SlidingWindowCounterSubWindowRateLimiter implements RateLimiter {
    private final int windowSizeInSeconds;
    private final int subWindowSizeInSeconds;
    private long maxPermits;
    private final ReentrantLock lock = new ReentrantLock();
    private final long[] subWindowCounters;
    private final long[] subWindowStartTimes;
    private int currentSubWindow;
    private double permitsPerSecond;

    public SlidingWindowCounterSubWindowRateLimiter(double permitsPerSecond, int windowSizeInSeconds, int subWindowSizeInSeconds) {
        this.permitsPerSecond = permitsPerSecond;
        this.maxPermits = (long) permitsPerSecond;
        this.windowSizeInSeconds = windowSizeInSeconds;
        this.subWindowSizeInSeconds = subWindowSizeInSeconds;
        int subWindowCount = (windowSizeInSeconds / subWindowSizeInSeconds) + 1;
        this.subWindowCounters = new long[subWindowCount];
        this.subWindowStartTimes = new long[subWindowCount];
        this.currentSubWindow = 0;
        long now = System.nanoTime();
        for (int i = 0; i < subWindowCount; i++) {
            subWindowStartTimes[i] = now;
        }
    }

    private void updateCounters() {
        long now = System.nanoTime();
        long windowDurationInNanos = windowSizeInSeconds * 1_000_000_000L;
        long subWindowDurationInNanos = subWindowSizeInSeconds * 1_000_000_000L;

        for (int i = 0; i < subWindowCounters.length; i++) {
            if (now - subWindowStartTimes[i] >= windowDurationInNanos) {
                subWindowCounters[i] = 0;
                subWindowStartTimes[i] = now;
            }
        }

        long oldestSubWindowStartTime = subWindowStartTimes[currentSubWindow];
        if (now - oldestSubWindowStartTime >= subWindowDurationInNanos) {
            currentSubWindow = (currentSubWindow + 1) % subWindowCounters.length;
            subWindowCounters[currentSubWindow] = 0;
            subWindowStartTimes[currentSubWindow] = now;
        }
    }

    private long getTotalCount() {
        long total = 0;
        for (long count : subWindowCounters) {
            total += count;
        }
        return total;
    }

    @Override
    public boolean acquire() {
        lock.lock();
        try {
            updateCounters();
            if (getTotalCount() < maxPermits) {
                subWindowCounters[currentSubWindow]++;
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
                updateCounters();
                if (getTotalCount() < maxPermits) {
                    subWindowCounters[currentSubWindow]++;
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
