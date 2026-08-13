package com.cfs.xnews.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducer {

    private final KafkaTemplate<String, ArticleEvent> kafkaTemplate;

    public KafkaProducer(
            KafkaTemplate<String, ArticleEvent> kafkaTemplate
    ) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishArticle(ArticleEvent event) {

        kafkaTemplate.send(
                KafkaTopicConfig.ARTICLE_TOPIC,
                String.valueOf(event.articleId()),
                event
        );
    }
}