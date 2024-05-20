package org.example;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        TokenBucketRateLimiter tokenBucketRateLimiter = new TokenBucketRateLimiter(/*capacity=*/ 5, /*permitsPerSeconds=*/ 1);
        // 5 tokens capacity, 1 token per second
        // Simulate 10 requests
        for (int i = 0; i < 10; i++) {
            System.out.println("Request " + (i + 1) + ": " + (tokenBucketRateLimiter.acquire() ? "Allowed" : "Denied"));
            Thread.sleep(200);
        }

        LeakyBucketRateLimiter leakyBucket = new LeakyBucketRateLimiter(5, 1); // 5 capacity, 1 permit per second

        // Simulate 10 requests
        for (int i = 0; i < 10; i++) {
            System.out.println("Request " + (i + 1) + ": " + (leakyBucket.acquire() ? "Allowed" : "Denied"));
            Thread.sleep(200);
        }

        FixedWindowRateLimiter fixedWindowRateLimiter = new FixedWindowRateLimiter(5); // 5 permits per second

        // Simulate 10 requests
        for (int i = 0; i < 10; i++) {
            System.out.println("Request " + (i + 1) + ": " + (fixedWindowRateLimiter.acquire() ? "Allowed" : "Denied"));
            Thread.sleep(200);
        }

        IPBasedRateLimiter ipBasedRateLimiter = new IPBasedRateLimiter(5); // 5 permits per second for each IP

        String ip1 = "192.168.1.1";
        String ip2 = "192.168.1.2";

        // Simulate 10 requests from two different IPs
        for (int i = 0; i < 10; i++) {
            System.out.println("Request " + (i + 1) + " from " + ip1 + ": " + (ipBasedRateLimiter.acquire(ip1) ? "Allowed" : "Denied"));
            System.out.println("Request " + (i + 1) + " from " + ip2 + ": " + (ipBasedRateLimiter.acquire(ip2) ? "Allowed" : "Denied"));
            Thread.sleep(200);
        }

    }
}