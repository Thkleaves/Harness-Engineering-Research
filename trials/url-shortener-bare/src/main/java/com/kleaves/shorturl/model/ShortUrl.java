package com.kleaves.shorturl.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

public class ShortUrl {
    private final String shortCode;
    private final String originalUrl;
    private final Instant createdAt;
    private final AtomicLong accessCount;

    public ShortUrl(String shortCode, String originalUrl) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = Instant.now();
        this.accessCount = new AtomicLong(0);
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getAccessCount() {
        return accessCount.get();
    }

    public long incrementAccessCount() {
        return accessCount.incrementAndGet();
    }
}
