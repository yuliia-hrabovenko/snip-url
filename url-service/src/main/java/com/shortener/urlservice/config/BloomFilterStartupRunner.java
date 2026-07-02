package com.shortener.urlservice.config;

import com.shortener.urlservice.repo.ShortUrlRepo;
import com.shortener.urlservice.service.ShortCodeBloomFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Bulk-loads every known short code into the Bloom filter at boot, so a freshly started
 * node can immediately reject unknown codes without waiting for individual cache/DB reads
 * to populate it lazily.
 */
@Component
public class BloomFilterStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BloomFilterStartupRunner.class);

    private final ShortCodeBloomFilter bloomFilter;
    private final ShortUrlRepo shortUrlRepo;

    public BloomFilterStartupRunner(ShortCodeBloomFilter bloomFilter, ShortUrlRepo shortUrlRepo) {
        this.bloomFilter = bloomFilter;
        this.shortUrlRepo = shortUrlRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> codes = shortUrlRepo.findAllShortCodes().stream()
                .map(ShortUrlRepo.ShortCodeOnly::getShortCode)
                .toList();
        bloomFilter.rebuild(codes);
        log.info("Bloom filter populated at startup with {} short codes", codes.size());
    }
}
