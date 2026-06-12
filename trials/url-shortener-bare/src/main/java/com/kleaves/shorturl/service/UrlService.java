package com.kleaves.shorturl.service;

import com.kleaves.shorturl.dto.UrlResponse;
import com.kleaves.shorturl.dto.UrlStatsResponse;
import com.kleaves.shorturl.model.ShortUrl;
import com.kleaves.shorturl.repository.UrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class UrlService {
    private static final String ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 7;
    private final SecureRandom random = new SecureRandom();

    private final UrlRepository repository;
    private final String baseUrl;

    public UrlService(UrlRepository repository, @Value("${short-url.base-url}") String baseUrl) {
        this.repository = repository;
        this.baseUrl = baseUrl;
    }

    public UrlResponse createShortUrl(String originalUrl) {
        String shortCode = generateShortCode();
        ShortUrl shortUrl = new ShortUrl(shortCode, originalUrl);
        repository.save(shortUrl);
        return new UrlResponse(baseUrl + "/" + shortCode, shortCode, originalUrl);
    }

    public Optional<String> resolveShortCode(String shortCode) {
        return repository.findByShortCode(shortCode).map(shortUrl -> {
            shortUrl.incrementAccessCount();
            return shortUrl.getOriginalUrl();
        });
    }

    public Optional<UrlStatsResponse> getStats(String shortCode) {
        return repository.findByShortCode(shortCode)
                .map(s -> new UrlStatsResponse(s.getShortCode(), s.getOriginalUrl(), s.getAccessCount(), s.getCreatedAt()));
    }

    private String generateShortCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
