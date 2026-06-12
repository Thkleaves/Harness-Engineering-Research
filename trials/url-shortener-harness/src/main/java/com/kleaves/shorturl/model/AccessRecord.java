package com.kleaves.shorturl.model;

import java.time.Instant;

public class AccessRecord {
    private final Instant timestamp;
    private final String referer;
    private final String userAgent;

    public AccessRecord(Instant timestamp, String referer, String userAgent) {
        this.timestamp = timestamp;
        this.referer = referer;
        this.userAgent = userAgent;
    }

    public Instant getTimestamp() { return timestamp; }
    public String getReferer() { return referer; }
    public String getUserAgent() { return userAgent; }
}
