package org.example;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LeakyBucketRateLimiter implements RateLimiter {
    private final ReentrantLock lock = new ReentrantLock();
    private final long capacity;
    private long permits;
    private long lastLeakTimestamp;
    private long leakIntervalInNanos;
    private double permitsPerSecond;

    public LeakyBucketRateLimiter(long capacity, double permitsPerSecond) {
        this.capacity = capacity;
        this.permitsPerSecond = permitsPerSecond;
        this.leakIntervalInNanos = (long) (1_000_000_000 / permitsPerSecond);
        this.permits = 0;
        this.lastLeakTimestamp = System.nanoTime();
    }

    @Override
    public boolean acquire() {
        lock.lock();
        try {
            leakPermitsIfNeeded();
            if (permits < capacity) {
                permits++;
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
                leakPermitsIfNeeded();
                if (permits < capacity) {
                    permits++;
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
            this.leakIntervalInNanos = (long) (1_000_000_000 / permitsPerSecond);
            leakPermitsIfNeeded();
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

    private void leakPermitsIfNeeded() {
        long now = System.nanoTime();
        if (now > lastLeakTimestamp) {
            long elapsedTime = now - lastLeakTimestamp;
            long leaks = elapsedTime / leakIntervalInNanos;
            if (leaks > 0) {
                permits = Math.max(0, permits - leaks);
                lastLeakTimestamp = now - (elapsedTime % leakIntervalInNanos);
            }
        }
    }

}

