package com.shortener.urlservice.service;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * In-memory guard layer checked before Redis/Mongo on every redirect lookup. If it says
 * a short code is definitely absent, the request is rejected immediately - this protects
 * the backend from scraping/random-guessing attacks that would otherwise generate a
 * cache-miss database query per attempt.
 *
 * Guava's BloomFilter can't delete entries or resize in place, so {@link #rebuild} does a
 * full atomic replace rather than an incremental removal.
 */
@Component
public class ShortCodeBloomFilter {

    private final long expectedInsertions;
    private final double falsePositiveProbability;
    private volatile BloomFilter<CharSequence> filter;

    public ShortCodeBloomFilter(
            @Value("${bloomfilter.expected-insertions}") long expectedInsertions,
            @Value("${bloomfilter.false-positive-probability}") double falsePositiveProbability) {
        this.expectedInsertions = expectedInsertions;
        this.falsePositiveProbability = falsePositiveProbability;
        this.filter = newFilter();
    }

    public boolean mightContain(String shortCode) {
        return filter.mightContain(shortCode);
    }

    public void put(String shortCode) {
        filter.put(shortCode);
    }

    public synchronized void rebuild(Iterable<String> allShortCodes) {
        BloomFilter<CharSequence> fresh = newFilter();
        allShortCodes.forEach(fresh::put);
        this.filter = fresh;
    }

    private BloomFilter<CharSequence> newFilter() {
        return BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), expectedInsertions, falsePositiveProbability);
    }
}
