package com.kleaves.shorturl.service;

import com.kleaves.shorturl.model.ShortUrl;

public class CreateResult {
    private final ShortUrl shortUrl;
    private final boolean reused;

    public CreateResult(ShortUrl shortUrl, boolean reused) {
        this.shortUrl = shortUrl;
        this.reused = reused;
    }

    public ShortUrl getShortUrl() { return shortUrl; }
    public boolean isReused() { return reused; }
}
