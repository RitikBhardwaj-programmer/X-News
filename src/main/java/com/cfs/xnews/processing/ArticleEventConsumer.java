package com.cfs.xnews.processing;

import com.cfs.xnews.kafka.ArticleEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ArticleEventConsumer {

    @KafkaListener(
            topics = "xnews.articles",
            groupId = "xnews-processing"
    )
    public void consume(ArticleEvent event) {

        System.out.println(
                "🔥 RECEIVED ARTICLE: " +
                        event.articleId() +
                        " | " +
                        event.title()
        );
    }
}