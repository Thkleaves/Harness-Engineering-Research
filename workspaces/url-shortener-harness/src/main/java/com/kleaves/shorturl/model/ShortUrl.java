package com.kleaves.shorturl.model;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

public class ShortUrl {
    private final String shortCode;
    private final String originalUrl;
    private final Instant createdAt;
    private final AtomicInteger accessCount = new AtomicInteger(0);
    private final Deque<AccessRecord> recentAccesses = new ConcurrentLinkedDeque<>();

    public ShortUrl(String shortCode, String originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = Instant.now();
    }

    public void recordAccess(Instant timestamp, String referer, String userAgent) {
        accessCount.incrementAndGet();
        recentAccesses.addLast(new AccessRecord(timestamp, referer, userAgent));
        while (recentAccesses.size() > 100) {
            recentAccesses.pollFirst();
        }
    }

    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public int getAccessCount() { return accessCount.get(); }
    public Deque<AccessRecord> getRecentAccesses() { return recentAccesses; }
}
