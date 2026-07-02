package com.shortener.urlservice.dto;

public record RedirectResult(String targetUrl, long cacheMaxAgeSeconds) {}
