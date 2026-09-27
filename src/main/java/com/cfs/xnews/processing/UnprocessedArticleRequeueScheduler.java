package com.cfs.xnews.processing;

import com.cfs.xnews.kafka.ArticleEvent;
import com.cfs.xnews.kafka.KafkaProducer;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Re-publishes articles whose processing message was lost (for example a
 * Kafka outage, or a record skipped after its retries). Only articles that
 * are at least 10 minutes old are touched, so normal in-flight work is left
 * alone; the 24-hour window bounds how long a permanently failing article
 * keeps being retried. Processing skips articles that are already done, so
 * a duplicate message is harmless.
 */
@Component
public class UnprocessedArticleRequeueScheduler {

    private static final Logger log = LoggerFactory.getLogger(UnprocessedArticleRequeueScheduler.class);

    static final long MIN_AGE_MINUTES = 10;
    static final long MAX_AGE_HOURS = 24;

    private final ArticleRepository articleRepository;
    private final KafkaProducer kafkaProducer;

    public UnprocessedArticleRequeueScheduler(
            ArticleRepository articleRepository,
            KafkaProducer kafkaProducer
    ) {
        this.articleRepository = articleRepository;
        this.kafkaProducer = kafkaProducer;
    }

    @Scheduled(initialDelay = 300_000, fixedDelay = 1_800_000)
    public void requeueUnprocessedArticles() {

        LocalDateTime now = LocalDateTime.now();

        List<Article> stuck = articleRepository.findByProcessedFalseAndCreatedAtBetween(
                now.minusHours(MAX_AGE_HOURS),
                now.minusMinutes(MIN_AGE_MINUTES)
        );

        for (Article article : stuck) {
            kafkaProducer.publishArticle(new ArticleEvent(
                    article.getId(),
                    article.getTitle(),
                    article.getDescription(),
                    article.getUrl(),
                    article.getSource(),
                    article.getPublishedAt()
            ));
        }

        if (!stuck.isEmpty()) {
            log.warn("Re-queued {} unprocessed article(s)", stuck.size());
        }
    }
}
