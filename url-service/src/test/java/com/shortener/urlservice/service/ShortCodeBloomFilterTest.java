package com.shortener.urlservice.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShortCodeBloomFilterTest {

    @Test
    void putThenMightContainReturnsTrue() {
        ShortCodeBloomFilter filter = new ShortCodeBloomFilter(1000, 0.0001);
        filter.put("abc123");
        assertTrue(filter.mightContain("abc123"));
    }

    @Test
    void unknownCodeIsRejectedGivenLowFalsePositiveRate() {
        ShortCodeBloomFilter filter = new ShortCodeBloomFilter(1000, 0.0001);
        filter.put("abc123");
        assertFalse(filter.mightContain("totallyUnknownCode"));
    }

    @Test
    void rebuildFullyReplacesContents() {
        ShortCodeBloomFilter filter = new ShortCodeBloomFilter(1000, 0.0001);
        filter.put("staleCode");
        assertTrue(filter.mightContain("staleCode"));

        filter.rebuild(List.of("freshCode"));

        assertFalse(filter.mightContain("staleCode"));
        assertTrue(filter.mightContain("freshCode"));
    }
}
