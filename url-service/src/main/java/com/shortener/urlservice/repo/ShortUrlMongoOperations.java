package com.shortener.urlservice.repo;

import com.shortener.urlservice.entity.ShortUrl;
import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Component;

/**
 * Atomic click-count increment. MongoRepository has no equivalent of JPA's
 * {@code @Modifying @Query}, so this wraps MongoTemplate's atomic $inc directly.
 */
@Component
public class ShortUrlMongoOperations {

    private final MongoTemplate mongoTemplate;

    public ShortUrlMongoOperations(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public long incrementClickCount(String shortCode) {
        Query query = Query.query(Criteria.where("shortCode").is(shortCode));
        Update update = new Update().inc("clickCount", 1);
        UpdateResult result = mongoTemplate.updateFirst(query, update, ShortUrl.class);
        return result.getModifiedCount();
    }

    public ShortUrl insert(ShortUrl shortUrl) {
        return mongoTemplate.insert(shortUrl);
    }
}
