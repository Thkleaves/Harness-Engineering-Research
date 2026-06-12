package com.kleaves.shorturl.controller;

import com.kleaves.shorturl.dto.CreateRequest;
import com.kleaves.shorturl.dto.CreateResponse;
import com.kleaves.shorturl.dto.ErrorResponse;
import com.kleaves.shorturl.dto.StatsResponse;
import com.kleaves.shorturl.model.ShortUrl;
import com.kleaves.shorturl.service.UrlShortenerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;

@RestController
public class UrlShortenerController {

    private final UrlShortenerService service;

    public UrlShortenerController(UrlShortenerService service) {
        this.service = service;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<?> createShortUrl(@Valid @RequestBody CreateRequest request,
                                            HttpServletRequest httpRequest) {
        var result = service.createShortUrl(request.getUrl());
        var shortUrl = result.getShortUrl();
        var fullShortUrl = buildShortUrl(httpRequest, shortUrl.getShortCode());

        var response = new CreateResponse(
                fullShortUrl,
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                result.isReused()
        );

        return ResponseEntity.status(result.isReused() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<?> redirect(@PathVariable String shortCode,
                                      @RequestHeader(value = "Referer", required = false) String referer,
                                      @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        var originalUrl = service.lookupAndRecord(shortCode, referer, userAgent);
        if (originalUrl.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Short URL not found"));
        }
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(originalUrl.get()))
                .build();
    }

    @GetMapping("/api/urls/{shortCode}/stats")
    public ResponseEntity<?> stats(@PathVariable String shortCode) {
        var result = service.getStats(shortCode);
        if (result.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Short URL not found"));
        }
        var shortUrl = result.get();
        var response = new StatsResponse(
                shortUrl.getShortCode(),
                shortUrl.getOriginalUrl(),
                shortUrl.getCreatedAt(),
                shortUrl.getAccessCount(),
                new ArrayList<>(shortUrl.getRecentAccesses())
        );
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    private String buildShortUrl(HttpServletRequest request, String shortCode) {
        var scheme = request.getScheme();
        var host = request.getServerName();
        var port = request.getServerPort();
        var base = scheme + "://" + host;
        if ((scheme.equals("http") && port != 80) || (scheme.equals("https") && port != 443)) {
            base += ":" + port;
        }
        return base + "/" + shortCode;
    }
}
