package com.kleaves.shorturl.dto;

import com.kleaves.shorturl.model.AccessRecord;

import java.time.Instant;
import java.util.List;

public class StatsResponse {
    private String shortCode;
    private String originalUrl;
    private Instant createdAt;
    private int accessCount;
    private List<AccessRecord> recentAccesses;

    public StatsResponse(String shortCode, String originalUrl, Instant createdAt, int accessCount, List<AccessRecord> recentAccesses) {
        this.shortCode = shortCode;
        this.originalUrl = originalUrl;
        this.createdAt = createdAt;
        this.accessCount = accessCount;
        this.recentAccesses = recentAccesses;
    }

    public String getShortCode() { return shortCode; }
    public String getOriginalUrl() { return originalUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public int getAccessCount() { return accessCount; }
    public List<AccessRecord> getRecentAccesses() { return recentAccesses; }
}
