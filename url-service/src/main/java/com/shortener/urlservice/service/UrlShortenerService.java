package com.shortener.urlservice.service;

import com.shortener.events.ClickEvent;
import com.shortener.urlservice.dto.RedirectResult;
import com.shortener.urlservice.dto.ShortUrlDto;
import com.shortener.urlservice.entity.ShortUrl;
import com.shortener.urlservice.repo.ShortUrlMongoOperations;
import com.shortener.urlservice.repo.ShortUrlRepo;
import com.shortener.urlservice.utils.Base62;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UrlShortenerService {
    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);
    private static final String CLICK_METRICS_TOPIC = "click-metrics";
    private static final String SHORT_CODE_CREATED_TOPIC = "short-code-created";
    private static final String CACHE_PREFIX = "short:";
    private static final Integer TTL = 60;

    private final ShortUrlRepo shortUrlRepo;
    private final ShortUrlMongoOperations shortUrlMongoOperations;
    private final RedisTemplate<String, String> redisTemplate;
    private final IdGenerator idGenerator;
    private final KafkaTemplate<String, ClickEvent> kafkaTemplate;
    private final KafkaTemplate<String, String> shortCodeKafkaTemplate;
    private final ShortCodeBloomFilter bloomFilter;
    private final long defaultCacheMaxAgeSeconds;

    public UrlShortenerService(ShortUrlRepo repository,
                               ShortUrlMongoOperations shortUrlMongoOperations,
                               IdGenerator idGenerator,
                               RedisTemplate<String, String> redisTemplate,
                               KafkaTemplate<String, ClickEvent> kafkaTemplate,
                               KafkaTemplate<String, String> shortCodeKafkaTemplate,
                               ShortCodeBloomFilter bloomFilter,
                               @Value("${cdn.default-max-age-seconds}") long defaultCacheMaxAgeSeconds) {
        this.shortUrlRepo = repository;
        this.shortUrlMongoOperations = shortUrlMongoOperations;
        this.idGenerator = idGenerator;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.shortCodeKafkaTemplate = shortCodeKafkaTemplate;
        this.bloomFilter = bloomFilter;
        this.defaultCacheMaxAgeSeconds = defaultCacheMaxAgeSeconds;
    }

    public ShortUrlDto shortenUrl(String targetUrl, Instant expiresAt) {
        Long generatedId = idGenerator.nextId();
        String shortCode = Base62.encode(generatedId);
        String key = CACHE_PREFIX + shortCode;

        ShortUrl shortUrlEntity = shortUrlMongoOperations.insert(new ShortUrl(generatedId, shortCode, targetUrl, expiresAt));
        bloomFilter.put(shortCode);
        redisTemplate.opsForValue().set(key, targetUrl, TTL, TimeUnit.MINUTES);
        broadcastShortCodeCreated(shortCode);
        return new ShortUrlDto(shortUrlEntity.getShortCode(), shortUrlEntity.getTargetUrl(), shortUrlEntity.getClickCount(), expiresAt);
    }

    public Optional<RedirectResult> redirect(String shortCode) {
        if (!bloomFilter.mightContain(shortCode)) {
            return Optional.empty();
        }

        String key = CACHE_PREFIX + shortCode;
        String targetUrl = redisTemplate.opsForValue().get(key);

        if (targetUrl != null) {
            triggerAsynchronousClickMetrics(shortCode);
            return Optional.of(new RedirectResult(targetUrl, TimeUnit.MINUTES.toSeconds(TTL)));
        }

        return shortUrlRepo.findByShortCode(shortCode)
                .filter(entity -> entity.getExpiresAt() == null || entity.getExpiresAt().isAfter(Instant.now()))
                .map(entity -> {
                    shortUrlMongoOperations.incrementClickCount(shortCode);

                    redisTemplate.opsForValue().set(
                            key,
                            entity.getTargetUrl(),
                            TTL,
                            TimeUnit.HOURS
                    );
                    return new RedirectResult(entity.getTargetUrl(), cacheMaxAgeSeconds(entity.getExpiresAt()));
                });
    }

    private long cacheMaxAgeSeconds(Instant expiresAt) {
        if (expiresAt == null) {
            return defaultCacheMaxAgeSeconds;
        }
        long secondsUntilExpiry = Instant.now().until(expiresAt, ChronoUnit.SECONDS);
        return Math.max(0, Math.min(defaultCacheMaxAgeSeconds, secondsUntilExpiry));
    }

    private void triggerAsynchronousClickMetrics(String shortCode) {
        kafkaTemplate.send(CLICK_METRICS_TOPIC, shortCode, new ClickEvent(shortCode, Instant.now()))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish click metric for shortCode={}", shortCode, ex);
                    }
                });
    }

    // Lets every other node's Bloom filter learn about this code within milliseconds,
    // instead of waiting for its next hourly rebuild from Mongo
    private void broadcastShortCodeCreated(String shortCode) {
        shortCodeKafkaTemplate.send(SHORT_CODE_CREATED_TOPIC, shortCode)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to broadcast short code creation for shortCode={}", shortCode, ex);
                    }
                });
    }
}
