package com.shortener.analytics.service;

import com.shortener.analytics.repo.ShortUrlMongoOperations;
import com.shortener.events.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ClickMetricsConsumerService {

    private static final Logger log = LoggerFactory.getLogger(ClickMetricsConsumerService.class);

    private final ShortUrlMongoOperations shortUrlMongoOperations;

    public ClickMetricsConsumerService(ShortUrlMongoOperations shortUrlMongoOperations) {
        this.shortUrlMongoOperations = shortUrlMongoOperations;
    }

    @KafkaListener(topics = "click-metrics", containerFactory = "clickEventListenerContainerFactory")
    public void handleClickEvent(ClickEvent event) {
        long updated = shortUrlMongoOperations.incrementClickCount(event.shortCode());
        if (updated == 0) {
            log.warn("Received click event for unknown shortCode={}", event.shortCode());
        } else {
            log.debug("Incremented click count for shortCode={}", event.shortCode());
        }
    }
}
