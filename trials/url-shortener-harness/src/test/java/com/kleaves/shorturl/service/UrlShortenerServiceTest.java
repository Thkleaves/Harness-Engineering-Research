package com.kleaves.shorturl.service;

import com.kleaves.shorturl.model.ShortUrl;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class UrlShortenerServiceTest {

    private final Clock fixedClock = Clock.fixed(Instant.parse("2026-06-11T10:00:00Z"), ZoneId.of("UTC"));

    @Test
    void shouldCreateNewShortUrl() {
        UrlShortenerService service = new UrlShortenerService(new RandomShortCodeGenerator(), fixedClock);
        CreateResult result = service.createShortUrl("https://example.com/hello");
        assertFalse(result.isReused());
        assertEquals("https://example.com/hello", result.getShortUrl().getOriginalUrl());
        assertEquals(7, result.getShortUrl().getShortCode().length());
    }

    @Test
    void shouldReuseExistingCodeWithin24Hours() {
        UrlShortenerService service = new UrlShortenerService(new RandomShortCodeGenerator(), fixedClock);
        ShortUrl first = service.createShortUrl("https://example.com/reuse-me").getShortUrl();
        CreateResult second = service.createShortUrl("https://example.com/reuse-me");
        assertEquals(first.getShortCode(), second.getShortUrl().getShortCode());
        assertTrue(second.isReused());
    }

    @Test
    void shouldCreateNewCodeAfter24Hours() {
        UrlShortenerService service = new UrlShortenerService(new RandomShortCodeGenerator(), fixedClock);
        ShortUrl first = service.createShortUrl("https://example.com/expired").getShortUrl();

        Clock laterClock = Clock.offset(fixedClock, Duration.ofHours(25));
        UrlShortenerService laterService = new UrlShortenerService(new RandomShortCodeGenerator(), laterClock);

        CreateResult second = laterService.createShortUrl("https://example.com/expired");
        assertNotEquals(first.getShortCode(), second.getShortUrl().getShortCode());
        assertFalse(second.isReused());
    }

    @Test
    void shouldLookupAndRecordAccess() {
        UrlShortenerService service = new UrlShortenerService(new RandomShortCodeGenerator(), fixedClock);
        String code = service.createShortUrl("https://example.com/track").getShortUrl().getShortCode();

        Optional<String> result = service.lookupAndRecord(code, "https://google.com", "Mozilla/5.0");
        assertTrue(result.isPresent());
        assertEquals("https://example.com/track", result.get());

        Optional<ShortUrl> stats = service.getStats(code);
        assertTrue(stats.isPresent());
        assertEquals(1, stats.get().getAccessCount());
        assertEquals(1, stats.get().getRecentAccesses().size());
        assertEquals("https://google.com", stats.get().getRecentAccesses().getFirst().getReferer());
    }

    @Test
    void shouldReturnEmptyForNonexistentCode() {
        UrlShortenerService service = new UrlShortenerService(new RandomShortCodeGenerator(), fixedClock);
        assertTrue(service.lookupAndRecord("no-such", null, null).isEmpty());
        assertTrue(service.getStats("no-such").isEmpty());
    }

    @Test
    void shouldNotExceed100RecentAccesses() {
        UrlShortenerService service = new UrlShortenerService(new RandomShortCodeGenerator(), fixedClock);
        String code = service.createShortUrl("https://example.com/many").getShortUrl().getShortCode();

        for (int i = 0; i < 150; i++) {
            service.lookupAndRecord(code, null, null);
        }

        Optional<ShortUrl> stats = service.getStats(code);
        assertTrue(stats.isPresent());
        assertEquals(150, stats.get().getAccessCount());
        assertEquals(100, stats.get().getRecentAccesses().size());
    }

    @Test
    void shouldRetryOnCollisionUpTo10Times() {
        ShortCodeGenerator colliding = () -> "AAAAAAA";
        UrlShortenerService service = new UrlShortenerService(colliding, fixedClock);

        assertNotNull(service.createShortUrl("https://example.com/one"));

        assertThrows(RuntimeException.class,
                () -> service.createShortUrl("https://example.com/two"));
    }

    @Test
    void shouldRejectInvalidUrls() {
        UrlShortenerService service = new UrlShortenerService(new RandomShortCodeGenerator(), fixedClock);
        assertThrows(IllegalArgumentException.class,
                () -> service.createShortUrl("not-a-valid-url"));
        assertThrows(IllegalArgumentException.class,
                () -> service.createShortUrl(""));
        assertThrows(IllegalArgumentException.class,
                () -> service.createShortUrl(null));
    }
}
