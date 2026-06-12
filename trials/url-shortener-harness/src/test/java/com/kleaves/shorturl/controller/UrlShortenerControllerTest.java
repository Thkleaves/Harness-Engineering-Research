package com.kleaves.shorturl.controller;

import com.kleaves.shorturl.model.ShortUrl;
import com.kleaves.shorturl.service.CreateResult;
import com.kleaves.shorturl.service.UrlShortenerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlShortenerController.class)
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UrlShortenerService service;

    @Test
    void shouldCreateShortUrlAndReturn201() throws Exception {
        var shortUrl = new ShortUrl("aBcDeF1", "https://example.com/long");
        when(service.createShortUrl("https://example.com/long"))
                .thenReturn(new CreateResult(shortUrl, false));

        mockMvc.perform(post("/api/urls")
                        .contentType("application/json")
                        .content("{\"url\": \"https://example.com/long\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("aBcDeF1"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/long"))
                .andExpect(jsonPath("$.reused").value(false));
    }

    @Test
    void shouldReuseExistingUrlAndReturn200() throws Exception {
        var shortUrl = new ShortUrl("aBcDeF1", "https://example.com/long");
        when(service.createShortUrl("https://example.com/long"))
                .thenReturn(new CreateResult(shortUrl, true));

        mockMvc.perform(post("/api/urls")
                        .contentType("application/json")
                        .content("{\"url\": \"https://example.com/long\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("aBcDeF1"))
                .andExpect(jsonPath("$.reused").value(true));
    }

    @Test
    void shouldReturn400ForInvalidUrl() throws Exception {
        when(service.createShortUrl("bad-url"))
                .thenThrow(new IllegalArgumentException("Invalid URL: bad-url"));

        mockMvc.perform(post("/api/urls")
                        .contentType("application/json")
                        .content("{\"url\": \"bad-url\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid URL: bad-url"));
    }

    @Test
    void shouldReturn302Redirect() throws Exception {
        when(service.lookupAndRecord(eq("aBcDeF1"), any(), any()))
                .thenReturn(Optional.of("https://example.com/target"));

        mockMvc.perform(get("/aBcDeF1"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("https://example.com/target"));
    }

    @Test
    void shouldReturn404ForNonexistentShortCode() throws Exception {
        when(service.lookupAndRecord(eq("noSuch"), any(), any()))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/noSuch"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Short URL not found"));
    }

    @Test
    void shouldReturnStats() throws Exception {
        var shortUrl = new ShortUrl("aBcDeF1", "https://example.com/long");
        when(service.getStats("aBcDeF1")).thenReturn(Optional.of(shortUrl));

        mockMvc.perform(get("/api/urls/aBcDeF1/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shortCode").value("aBcDeF1"))
                .andExpect(jsonPath("$.originalUrl").value("https://example.com/long"))
                .andExpect(jsonPath("$.accessCount").value(0));
    }

    @Test
    void shouldReturn404ForNonexistentStats() throws Exception {
        when(service.getStats("noSuch")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/urls/noSuch/stats"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Short URL not found"));
    }
}
