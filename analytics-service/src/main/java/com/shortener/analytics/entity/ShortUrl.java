package com.shortener.analytics.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "short_urls")
public class ShortUrl {

    @Id
    private Long id;

    @Field("short_code")
    private String shortCode;

    @Field("click_count")
    private Long clickCount = 0L;

    public Long getId() { return id; }
    public String getShortCode() { return shortCode; }
    public Long getClickCount() { return clickCount; }
}
