package com.kleaves.shorturl.repository;

import com.kleaves.shorturl.model.ShortUrl;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class UrlRepository {
    private final Map<String, ShortUrl> store = new ConcurrentHashMap<>();

    public void save(ShortUrl shortUrl) {
        store.put(shortUrl.getShortCode(), shortUrl);
    }

    public Optional<ShortUrl> findByShortCode(String shortCode) {
        return Optional.ofNullable(store.get(shortCode));
    }
}
