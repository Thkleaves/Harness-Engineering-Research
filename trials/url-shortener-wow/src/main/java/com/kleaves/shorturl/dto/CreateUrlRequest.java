package com.kleaves.shorturl.dto;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateUrlRequest(@NotBlank @URL String url) {}
