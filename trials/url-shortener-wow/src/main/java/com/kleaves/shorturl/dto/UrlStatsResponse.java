package com.kleaves.shorturl.dto;

import java.time.Instant;

public record UrlStatsResponse(String shortCode, String originalUrl, long accessCount, Instant createdAt) {}
