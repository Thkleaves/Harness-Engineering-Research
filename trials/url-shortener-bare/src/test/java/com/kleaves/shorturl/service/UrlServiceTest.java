package com.kleaves.shorturl.service;

import com.kleaves.shorturl.dto.UrlResponse;
import com.kleaves.shorturl.dto.UrlStatsResponse;
import com.kleaves.shorturl.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class UrlServiceTest {
    private static final String BASE_URL = "http://localhost:8080";
    private UrlService service;
    private UrlRepository repository;

    @BeforeEach
    void setUp() {
        repository = new UrlRepository();
        service = new UrlService(repository, BASE_URL);
    }

    @Test
    void shouldCreateShortUrl() {
        UrlResponse response = service.createShortUrl("https://example.com");

        assertThat(response.originalUrl()).isEqualTo("https://example.com");
        assertThat(response.shortCode()).hasSize(7);
        assertThat(response.shortUrl()).isEqualTo(BASE_URL + "/" + response.shortCode());
    }

    @Test
    void shouldResolveShortCode() {
        UrlResponse created = service.createShortUrl("https://example.com");

        Optional<String> resolved = service.resolveShortCode(created.shortCode());

        assertThat(resolved).hasValue("https://example.com");
    }

    @Test
    void shouldReturnEmptyForUnknownShortCode() {
        Optional<String> resolved = service.resolveShortCode("unknown");

        assertThat(resolved).isEmpty();
    }

    @Test
    void shouldIncrementAccessCountOnResolve() {
        UrlResponse created = service.createShortUrl("https://example.com");

        service.resolveShortCode(created.shortCode());
        service.resolveShortCode(created.shortCode());

        Optional<UrlStatsResponse> stats = service.getStats(created.shortCode());
        assertThat(stats).isPresent();
        assertThat(stats.get().accessCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnStats() {
        UrlResponse created = service.createShortUrl("https://example.com");

        Optional<UrlStatsResponse> stats = service.getStats(created.shortCode());

        assertThat(stats).isPresent();
        assertThat(stats.get().shortCode()).isEqualTo(created.shortCode());
        assertThat(stats.get().originalUrl()).isEqualTo("https://example.com");
        assertThat(stats.get().accessCount()).isZero();
        assertThat(stats.get().createdAt()).isNotNull();
    }

    @Test
    void shouldReturnEmptyStatsForUnknownShortCode() {
        Optional<UrlStatsResponse> stats = service.getStats("unknown");
        assertThat(stats).isEmpty();
    }

    @Test
    void eachShortCodeShouldBeUnique() {
        UrlResponse r1 = service.createShortUrl("https://a.com");
        UrlResponse r2 = service.createShortUrl("https://b.com");

        assertThat(r1.shortCode()).isNotEqualTo(r2.shortCode());
    }
}
