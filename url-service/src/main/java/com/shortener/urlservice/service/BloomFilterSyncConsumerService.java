package com.shortener.urlservice.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class BloomFilterSyncConsumerService {

    private final ShortCodeBloomFilter bloomFilter;

    public BloomFilterSyncConsumerService(ShortCodeBloomFilter bloomFilter) {
        this.bloomFilter = bloomFilter;
    }

    @KafkaListener(topics = "short-code-created", containerFactory = "shortCodeCreatedListenerContainerFactory")
    public void handleShortCodeCreated(String shortCode) {
        bloomFilter.put(shortCode);
    }
}
