package com.shortener.urlservice.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.Instant;

@Document(collection = "short_urls")
public class ShortUrl {

    @Id
    private Long id;

    @Field("short_code")
    private String shortCode;

    @Field("target_url")
    private String targetUrl;

    @Field("click_count")
    private Long clickCount = 0L;

    @Field("expires_at")
    private Instant expiresAt;

    @Field("created_at")
    private Instant createdAt = Instant.now();

    public ShortUrl() {}

    public ShortUrl(Long id, String shortCode, String targetUrl, Instant expiresAt) {
        this.id = id;
        this.shortCode = shortCode;
        this.targetUrl = targetUrl;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public String getTargetUrl() { return targetUrl; }
    public Long getClickCount() { return clickCount; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void incrementClickCount() {
        this.clickCount++;
    }
}
