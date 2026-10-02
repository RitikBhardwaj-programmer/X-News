package com.cfs.xnews.processing;

import com.cfs.xnews.claim.ClaimService;
import com.cfs.xnews.entity.EntityService;
import com.cfs.xnews.kafka.ArticleEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ArticleEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(ArticleEventConsumer.class);

    private final ArticleProcessingService processingService;
    private final EntityService entityService;
    private final ClaimService claimService;

    public ArticleEventConsumer(
            ArticleProcessingService processingService,
            EntityService entityService,
            ClaimService claimService
    ) {
        this.processingService = processingService;
        this.entityService = entityService;
        this.claimService = claimService;
    }

    @KafkaListener(
            topics = "xnews.articles",
            groupId = "xnews-processing"
    )
    public void consume(ArticleEvent event) {

        processingService.process(event);

        // Entities are extra context: a failure is logged, never retried, and
        // never undoes the processing above.
        try {
            entityService.recordMentions(event.articleId());
        } catch (RuntimeException e) {
            log.warn("Recording entities failed for article={}: {}", event.articleId(), e.getMessage());
        }

        // Claims likewise (cricket only; reviewed before display).
        try {
            claimService.recordClaims(event.articleId());
        } catch (RuntimeException e) {
            log.warn("Recording claims failed for article={}: {}", event.articleId(), e.getMessage());
        }
    }
}
