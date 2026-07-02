package com.shortener.urlservice.service;

import com.shortener.urlservice.repo.ShortUrlRepo;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Periodically rebuilds the Bloom filter from scratch. Guava's BloomFilter can't delete
 * individual entries, so expired/removed codes would otherwise linger forever and the
 * false-positive rate would only ever grow as the dataset changes - a full rebuild bounds
 * both.
 */
@Component
public class BloomFilterRebuildScheduler {

    private final ShortCodeBloomFilter bloomFilter;
    private final ShortUrlRepo shortUrlRepo;

    public BloomFilterRebuildScheduler(ShortCodeBloomFilter bloomFilter, ShortUrlRepo shortUrlRepo) {
        this.bloomFilter = bloomFilter;
        this.shortUrlRepo = shortUrlRepo;
    }

    @Scheduled(fixedRateString = "${bloomfilter.rebuild-interval-ms}")
    public void rebuild() {
        List<String> codes = shortUrlRepo.findAllShortCodes().stream()
                .map(ShortUrlRepo.ShortCodeOnly::getShortCode)
                .toList();
        bloomFilter.rebuild(codes);
    }
}
