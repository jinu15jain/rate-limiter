package org.example;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class SlidingWindowLogRateLimiter implements RateLimiter {
    private final ReentrantLock lock = new ReentrantLock();

    private final long windowSizeInNanos;
    private final Deque<Long> timestamps;
    private double permitsPerSecond;
    private long maxPermits;

    public SlidingWindowLogRateLimiter(double permitsPerSecond) {
        this.permitsPerSecond = permitsPerSecond;
        this.maxPermits = (long) permitsPerSecond;
        this.windowSizeInNanos = 1_000_000_000; // 1 second in nanoseconds
        this.timestamps = new LinkedList<>();
    }

    @Override
    public boolean acquire() {
        long now = System.nanoTime();
        lock.lock();
        try {
            // Remove timestamps that are outside the window
            while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowSizeInNanos) {
                timestamps.pollFirst();
            }

            if (timestamps.size() < maxPermits) {
                timestamps.addLast(now);
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
                // Remove timestamps that are outside the window
                while (!timestamps.isEmpty() && now - timestamps.peekFirst() >= windowSizeInNanos) {
                    timestamps.pollFirst();
                }

                if (timestamps.size() < maxPermits) {
                    timestamps.addLast(now);
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
