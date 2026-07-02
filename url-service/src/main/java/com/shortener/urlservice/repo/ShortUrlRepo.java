package com.shortener.urlservice.repo;

import com.shortener.urlservice.entity.ShortUrl;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ShortUrlRepo extends MongoRepository<ShortUrl, Long> {
    Optional<ShortUrl> findByShortCode(String shortCode);
    boolean existsByShortCode(String shortCode);

    @Query(value = "{}", fields = "{ 'short_code' : 1, '_id' : 0 }")
    List<ShortCodeOnly> findAllShortCodes();

    interface ShortCodeOnly {
        String getShortCode();
    }
}
