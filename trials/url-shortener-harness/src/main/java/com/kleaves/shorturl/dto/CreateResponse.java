package com.kleaves.shorturl.dto;

import java.time.Instant;

public class CreateResponse {
    private String shortUrl;
    private String shortCode;
    private String originalUrl;
    private Instant createdAt;
    private boolean reused;

    public CreateResponse(String shortUrl, String shortCode, String originalUrl, Instant createdAt, boolean reused) {
        this.shortUrl = shortUrl;
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.reused = reused;
    }

    public String getShortUrl() { return shortUrl; }
    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isReused() { return reused; }
}
