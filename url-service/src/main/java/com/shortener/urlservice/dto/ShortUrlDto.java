package com.shortener.urlservice.dto;

import java.time.Instant;

public class ShortUrlDto {

    private String shortCode;

    private String targetUrl;

    private Long clickCount;

    private Instant expiresAt;

    public ShortUrlDto() {}

    public ShortUrlDto(String shortCode, String targetUrl, Long clickCount, Instant expiresAt) {
        this.clickCount = clickCount;
        this.shortCode = shortCode;
        this.targetUrl = targetUrl;
        this.expiresAt = expiresAt;
    }

    public String getShortCode() { return shortCode; }
    public String getTargetUrl() { return targetUrl; }
    public Long getClickCount() { return clickCount; }
    public Instant getExpiresAt() { return expiresAt; }
}
