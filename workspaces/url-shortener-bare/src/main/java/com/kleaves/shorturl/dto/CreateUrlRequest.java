package com.kleaves.shorturl.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateUrlRequest(@NotBlank String url) {}
