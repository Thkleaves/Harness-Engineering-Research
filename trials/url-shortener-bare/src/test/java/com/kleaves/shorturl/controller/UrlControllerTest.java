package com.kleaves.shorturl.controller;

import com.kleaves.shorturl.dto.CreateUrlRequest;
import com.kleaves.shorturl.dto.UrlResponse;
import com.kleaves.shorturl.dto.UrlStatsResponse;
import com.kleaves.shorturl.service.UrlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlController.class)
class UrlControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlService service;

    @Test
    void shouldCreateShortUrl() throws Exception {
        when(service.createShortUrl(any())).thenReturn(
                new UrlResponse("http://localhost:8080/abc1234", "abc1234", "https://example.com"));

        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"https://example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/abc1234"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"));
    }

    @Test
    void shouldRejectEmptyUrl() throws Exception {
        mockMvc.perform(post("/api/urls")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRedirect() throws Exception {
        when(service.resolveShortCode("abc1234")).thenReturn(Optional.of("https://example.com"));

        mockMvc.perform(get("/abc1234"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com"));
    }

    @Test
    void shouldReturn404ForUnknownShortCode() throws Exception {
        when(service.resolveShortCode("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnStats() throws Exception {
        Instant now = Instant.now();
        when(service.getStats("abc1234")).thenReturn(Optional.of(
                new UrlStatsResponse("abc1234", "https://example.com", 5, now)));

        mockMvc.perform(get("/api/urls/abc1234/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("abc1234"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com"))
                .andExpect(jsonPath("$.accessCount").value(5));
    }

    @Test
    void shouldReturn404ForUnknownStats() throws Exception {
        when(service.getStats("unknown")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/urls/unknown/stats"))
                .andExpect(status().isNotFound());
    }
}
