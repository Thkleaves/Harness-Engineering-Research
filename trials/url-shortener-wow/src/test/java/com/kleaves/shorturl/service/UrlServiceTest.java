package com.kleaves.shorturl.service;

import com.kleaves.shorturl.dto.UrlResponse;
import com.kleaves.shorturl.dto.UrlStatsResponse;
import com.kleaves.shorturl.model.ShortUrl;
import com.kleaves.shorturl.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void shouldRetryOnCollision() {
        var collidingService = new UrlService(repository, BASE_URL) {
            private int calls = 0;

            @Override
            String generateShortCode() {
                calls++;
                if (calls == 1) return "collision";
                return "unique99";
            }
        };
        repository.save(new ShortUrl("collision", "https://first.com"));

        UrlResponse response = collidingService.createShortUrl("https://second.com");

        assertThat(response.shortCode()).isEqualTo("unique99");
        assertThat(repository.findByShortCode("collision")).isPresent();
        assertThat(repository.findByShortCode("collision").get().getOriginalUrl()).isEqualTo("https://first.com");
    }

    @Test
    void shouldThrowWhenMaxRetriesExceeded() {
        repository.save(new ShortUrl("a", "https://a.com"));
        repository.save(new ShortUrl("b", "https://b.com"));
        repository.save(new ShortUrl("c", "https://c.com"));

        var collidingService = new UrlService(repository, BASE_URL) {
            private int calls = 0;
            private final String[] codes = {"a", "b", "c"};

            @Override
            String generateShortCode() {
                return codes[calls++];
            }
        };

        assertThatThrownBy(() -> collidingService.createShortUrl("https://new.com"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("status").isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shortCodeShouldContainOnlyValidCharacters() {
        String allowed = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Set<Character> allowedSet = allowed.chars().mapToObj(c -> (char) c).collect(Collectors.toSet());

        UrlResponse response = service.createShortUrl("https://example.com");

        assertThat(response.shortCode()).hasSize(7);
        for (char c : response.shortCode().toCharArray()) {
            assertThat(allowedSet).contains(c);
        }
    }
}
