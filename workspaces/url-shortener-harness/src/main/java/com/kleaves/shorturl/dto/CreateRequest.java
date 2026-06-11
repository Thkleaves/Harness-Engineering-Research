package com.kleaves.shorturl.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateRequest {
    @NotBlank
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
