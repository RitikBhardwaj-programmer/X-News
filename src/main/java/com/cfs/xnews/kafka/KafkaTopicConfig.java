package com.cfs.xnews.kafka;

import org.apache.kafka.clients.admin.NewTopic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ARTICLE_TOPIC =
            "xnews.articles";

    @Bean
    public NewTopic articleTopic() {

        return TopicBuilder
                .name(ARTICLE_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }
}