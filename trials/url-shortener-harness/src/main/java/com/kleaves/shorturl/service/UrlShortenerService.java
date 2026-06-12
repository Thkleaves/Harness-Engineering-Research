package com.kleaves.shorturl.service;

import com.kleaves.shorturl.model.ShortUrl;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class UrlShortenerService {

    private static final int MAX_RETRIES = 10;
    private static final Duration DEDUP_WINDOW = Duration.ofHours(24);

    private final ConcurrentHashMap<String, ShortUrl> store = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ShortUrl> urlIndex = new ConcurrentHashMap<>();
    private final ShortCodeGenerator generator;
    private final Clock clock;

    public UrlShortenerService(ShortCodeGenerator generator, Clock clock) {
        this.generator = generator;
        this.clock = clock;
    }

    public CreateResult createShortUrl(String originalUrl) {
        if (originalUrl == null || originalUrl.isBlank()) {
            throw new IllegalArgumentException("URL must not be empty");
        }
        if (!originalUrl.startsWith("http://") && !originalUrl.startsWith("https://")) {
            throw new IllegalArgumentException("Invalid URL: " + originalUrl);
        }
        var normalized = normalizeUrl(originalUrl);
        if (!isValidUrl(normalized)) {
            throw new IllegalArgumentException("Invalid URL: " + originalUrl);
        }

        // Check 24h dedup
        var existing = urlIndex.get(normalized);
        if (existing != null) {
            var age = Duration.between(existing.getCreatedAt(), Instant.now(clock));
            if (age.compareTo(DEDUP_WINDOW) < 0) {
                return new CreateResult(existing, true);
            }
            // Expired, remove old index entry
            urlIndex.remove(normalized, existing);
        }

        for (int i = 0; i < MAX_RETRIES; i++) {
            var code = generator.generate();
            var shortUrl = new ShortUrl(code, normalized);
            var previous = store.putIfAbsent(code, shortUrl);
            if (previous == null) {
                urlIndex.put(normalized, shortUrl);
                return new CreateResult(shortUrl, false);
            }
        }

        throw new RuntimeException("Failed to generate unique short code after " + MAX_RETRIES + " attempts");
    }

    public Optional<String> lookupAndRecord(String shortCode, String referer, String userAgent) {
        var shortUrl = store.get(shortCode);
        if (shortUrl == null) {
            return Optional.empty();
        }
        shortUrl.recordAccess(Instant.now(clock), referer, userAgent);
        return Optional.of(shortUrl.getOriginalUrl());
    }

    public Optional<ShortUrl> getStats(String shortCode) {
        return Optional.ofNullable(store.get(shortCode));
    }

    private String normalizeUrl(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    private boolean isValidUrl(String url) {
        try {
            var uri = new URI(url);
            return uri.getHost() != null && !uri.getHost().isBlank();
        } catch (Exception e) {
            return false;
        }
    }
}
