package com.shortener.urlservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shortener.events.ClickEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public NewTopic clickMetricsTopic() {
        return TopicBuilder.name("click-metrics")
                .partitions(3)
                .replicas(3)
                .build();
    }

    @Bean
    public ProducerFactory<String, ClickEvent> clickEventProducerFactory(ObjectMapper objectMapper) {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.ACKS_CONFIG, "1"
                ),
                new StringSerializer(),
                new JacksonJsonSerializer<>()
        );
    }

    @Bean
    public KafkaTemplate<String, ClickEvent> clickEventKafkaTemplate(
            ProducerFactory<String, ClickEvent> clickEventProducerFactory) {
        return new KafkaTemplate<>(clickEventProducerFactory);
    }

    @Bean
    public NewTopic shortCodeCreatedTopic() {
        return TopicBuilder.name("short-code-created")
                .partitions(1)
                .replicas(3)
                .build();
    }

    // Broadcasts newly-created short codes so every node's in-memory Bloom filter learns
    // about them within milliseconds instead of waiting for the hourly rebuild
    @Bean
    public ProducerFactory<String, String> shortCodeProducerFactory() {
        return new DefaultKafkaProducerFactory<>(
                Map.of(
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ProducerConfig.ACKS_CONFIG, "1"
                ),
                new StringSerializer(),
                new StringSerializer()
        );
    }

    @Bean
    public KafkaTemplate<String, String> shortCodeKafkaTemplate(
            ProducerFactory<String, String> shortCodeProducerFactory) {
        return new KafkaTemplate<>(shortCodeProducerFactory);
    }
}
