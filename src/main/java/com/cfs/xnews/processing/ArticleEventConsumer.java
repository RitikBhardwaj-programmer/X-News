package com.cfs.xnews.processing;

import com.cfs.xnews.kafka.ArticleEvent;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ArticleEventConsumer {

    private final ArticleProcessingService processingService;

    public ArticleEventConsumer(
            ArticleProcessingService processingService
    ) {
        this.processingService = processingService;
    }

    @KafkaListener(
            topics = "xnews.articles",
            groupId = "xnews-processing"
    )
    public void consume(ArticleEvent event) {

        processingService.process(event);
    }
}