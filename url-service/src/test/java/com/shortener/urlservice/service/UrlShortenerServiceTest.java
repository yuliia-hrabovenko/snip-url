package com.shortener.urlservice.service;

import com.shortener.events.ClickEvent;
import com.shortener.urlservice.dto.RedirectResult;
import com.shortener.urlservice.repo.ShortUrlMongoOperations;
import com.shortener.urlservice.repo.ShortUrlRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlShortenerServiceTest {

    @Mock
    private ShortUrlRepo shortUrlRepo;
    @Mock
    private ShortUrlMongoOperations shortUrlMongoOperations;
    @Mock
    private IdGenerator idGenerator;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private KafkaTemplate<String, ClickEvent> kafkaTemplate;
    @Mock
    private KafkaTemplate<String, String> shortCodeKafkaTemplate;
    @Mock
    private ShortCodeBloomFilter bloomFilter;

    private UrlShortenerService service;

    @BeforeEach
    void setUp() {
        service = new UrlShortenerService(
                shortUrlRepo, shortUrlMongoOperations, idGenerator, redisTemplate, kafkaTemplate,
                shortCodeKafkaTemplate, bloomFilter, 86400L);
    }

    @Test
    void redirectShortCircuitsWithoutTouchingRedisOrMongoWhenBloomFilterRejects() {
        when(bloomFilter.mightContain("unknown")).thenReturn(false);

        Optional<RedirectResult> result = service.redirect("unknown");

        assertTrue(result.isEmpty());
        verifyNoInteractions(redisTemplate);
        verifyNoInteractions(shortUrlRepo);
    }

    @Test
    void redirectReturnsTargetUrlFromRedisOnCacheHit() {
        when(bloomFilter.mightContain("known")).thenReturn(true);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("short:known")).thenReturn("https://example.com");
        when(kafkaTemplate.send(anyString(), anyString(), any(ClickEvent.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        Optional<RedirectResult> result = service.redirect("known");

        assertTrue(result.isPresent());
        assertEquals("https://example.com", result.get().targetUrl());
        verifyNoInteractions(shortUrlRepo);
    }
}
