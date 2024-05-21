package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FixedWindowRateLimiterTest {

    private FixedWindowRateLimiter rateLimiter;

    @BeforeEach
    public void setUp() {
        rateLimiter = new FixedWindowRateLimiter(5); // 5 permits per second
    }

    @Test
    public void testBasicRateLimiting() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.acquire());
        }
        assertFalse(rateLimiter.acquire());

        // Wait for the next window
        TimeUnit.SECONDS.sleep(1);
        assertTrue(rateLimiter.acquire());
    }

    @Test
    public void testAcquireWithTimeout() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimiter.acquire());
        }
        assertFalse(rateLimiter.tryAcquire(100, TimeUnit.MILLISECONDS));

        // Wait for the next window
        TimeUnit.SECONDS.sleep(1);
        assertTrue(rateLimiter.tryAcquire(100, TimeUnit.MILLISECONDS));
    }

    @Test
    public void testChangeRate() throws InterruptedException {
        rateLimiter.setRate(10); // Change rate to 10 permits per second
        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimiter.acquire());
        }
        assertFalse(rateLimiter.acquire());

        // Wait for the next window
        TimeUnit.SECONDS.sleep(1);
        assertTrue(rateLimiter.acquire());
    }

    @Test
    public void testGetRate() {
        assertEquals(5, rateLimiter.getRate());
        rateLimiter.setRate(10);
        assertEquals(10, rateLimiter.getRate());
    }

    @Test
    public void testConcurrency() throws InterruptedException {
        rateLimiter.setRate(50); // 50 permits per second for concurrency test
        AtomicInteger successfulAcquires = new AtomicInteger(0);
        AtomicInteger failedAcquires = new AtomicInteger(0);

        int numberOfThreads = 10;
        int acquiresPerThread = 10;

        Runnable acquireTask = () -> {
            for (int i = 0; i < acquiresPerThread; i++) {
                if (rateLimiter.acquire()) {
                    successfulAcquires.incrementAndGet();
                } else {
                    failedAcquires.incrementAndGet();
                }
            }
        };

        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(acquireTask);
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(50, successfulAcquires.get());
        assertEquals(50, successfulAcquires.get());

    }

    @Test
    public void testConcurrencyV2() throws InterruptedException {
        rateLimiter.setRate(20);

        AtomicInteger successfulAcquires = new AtomicInteger(0);
        AtomicInteger failureAcquire = new AtomicInteger(0);

        Runnable acquireTask = () -> {
            for(int i= 0; i < 50; i++) {
                if (rateLimiter.acquire()) {
                    successfulAcquires.incrementAndGet();
                } else {
                    failureAcquire.incrementAndGet();
                }
            }
        };

        Thread[] threads = new Thread[10];

        for(int i = 0; i < 10; i++) {
            threads[i] = new Thread(acquireTask);
            threads[i].start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        assertEquals(20, successfulAcquires.get());
        assertEquals(480, failureAcquire.get());

    }
}
