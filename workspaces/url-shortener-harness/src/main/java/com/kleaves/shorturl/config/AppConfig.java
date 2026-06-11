package com.kleaves.shorturl.config;

import com.kleaves.shorturl.service.RandomShortCodeGenerator;
import com.kleaves.shorturl.service.ShortCodeGenerator;
import com.kleaves.shorturl.service.UrlShortenerService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public ShortCodeGenerator shortCodeGenerator() {
        return new RandomShortCodeGenerator();
    }

    @Bean
    public UrlShortenerService urlShortenerService(ShortCodeGenerator generator, Clock clock) {
        return new UrlShortenerService(generator, clock);
    }
}
