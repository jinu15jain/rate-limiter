package org.example;

import java.util.concurrent.TimeUnit;

public interface RateLimiter {

    /**
     * Acquires a permit from the rate limiter, blocking until one is available.
     *
     * @return true if the permit was successfully acquired, false otherwise.
     */
    boolean acquire();

    /**
     * Tries to acquire a permit from the rate limiter, blocking until one is available
     * or the timeout expires.
     *
     * @param timeout the maximum time to wait for a permit
     * @param unit the time unit of the timeout argument
     * @return true if the permit was successfully acquired, false if the timeout expired
     */
    boolean tryAcquire(long timeout, TimeUnit unit);

    /**
     * Sets the rate of the rate limiter.
     *
     * @param permitsPerSecond the number of permits to allow per second
     */
    void setRate(double permitsPerSecond);

    /**
     * Gets the current rate of the rate limiter.
     *
     * @return the number of permits allowed per second
     */
    double getRate();
}
