package com.cfs.xnews.processing;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventService;
import com.cfs.xnews.event.dto.EmbeddingResponse;
import com.cfs.xnews.event.dto.EventMatchRequest;
import com.cfs.xnews.event.dto.EventMatchResponse;
import com.cfs.xnews.kafka.ArticleEvent;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.processing.processor.CategoryProcessor;
import com.cfs.xnews.processing.processor.ContentCleaner;
import com.cfs.xnews.processing.processor.KeywordProcessor;
import com.cfs.xnews.processing.processor.SentimentProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
public class ArticleProcessingService {

    private final NewsEventService newsEventService;
    private final SentimentProcessor sentimentProcessor;
    private final CategoryProcessor categoryProcessor;
    private final KeywordProcessor keywordProcessor;
    private final ArticleRepository articleRepository;
    private final ContentCleaner contentCleaner;
    private final EventMatchingClient eventMatchingClient;

    private final double eventMatchThreshold;

    public ArticleProcessingService(
            NewsEventService newsEventService,
            SentimentProcessor sentimentProcessor,
            CategoryProcessor categoryProcessor,
            KeywordProcessor keywordProcessor,
            ArticleRepository articleRepository,
            ContentCleaner contentCleaner,
            EventMatchingClient eventMatchingClient,

            @Value("${ai.event-matcher.threshold:0.70}")
            double eventMatchThreshold
    ) {

        this.newsEventService = newsEventService;
        this.sentimentProcessor = sentimentProcessor;
        this.categoryProcessor = categoryProcessor;
        this.keywordProcessor = keywordProcessor;
        this.articleRepository = articleRepository;
        this.contentCleaner = contentCleaner;
        this.eventMatchingClient = eventMatchingClient;
        this.eventMatchThreshold = eventMatchThreshold;
    }

    @Transactional
    public void process(ArticleEvent event) {

        Article article = articleRepository
                .findById(event.articleId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Article not found: " +
                                        event.articleId()
                        )
                );

        // =====================================================
        // ALREADY PROCESSED
        // =====================================================

        if (article.isProcessed()) {

            System.out.println(
                    "Skipping already processed article: " +
                            article.getId()
            );

            return;
        }

        // =====================================================
        // 1. CLEAN CONTENT
        // =====================================================

        String cleanedDescription =
                contentCleaner.clean(
                        article.getDescription()
                );

        article.setDescription(
                cleanedDescription
        );

        // =====================================================
        // 2. CATEGORY
        // =====================================================

        article.setCategory(
                categoryProcessor.classify(article)
        );

        // =====================================================
        // 3. KEYWORDS
        // =====================================================

        article.setKeywords(
                keywordProcessor.extract(article)
        );

        // =====================================================
        // 4. SENTIMENT
        // =====================================================

        article.setSentiment(
                sentimentProcessor.analyze(article)
        );

        // =====================================================
        // 5. GENERATE EMBEDDING
        // =====================================================

        String embeddingText =
                article.getTitle()
                        + "\n"
                        + article.getDescription();

        EmbeddingResponse embeddingResponse =
                eventMatchingClient.generateEmbedding(
                        embeddingText
                );

        List<Double> values =
                embeddingResponse.embedding();

        if (values == null || values.size() != 384) {

            throw new RuntimeException(
                    "Invalid embedding received. Expected 384 dimensions but got: "
                            + (values == null ? 0 : values.size())
            );
        }

        float[] embedding =
                new float[values.size()];

        for (int i = 0; i < values.size(); i++) {

            embedding[i] =
                    values.get(i).floatValue();
        }

        article.setEmbedding(embedding);

        // =====================================================
        // 6. SAVE ARTICLE + EMBEDDING
        // =====================================================

        articleRepository.save(article);

        // =====================================================
        // 7. FIND NEAREST ARTICLES USING PGVECTOR
        // =====================================================

        StringBuilder vectorBuilder = new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {

            if (i > 0) {
                vectorBuilder.append(",");
            }

            vectorBuilder.append(embedding[i]);
        }

        vectorBuilder.append("]");

        String pgVector = vectorBuilder.toString();

        List<Object[]> nearestArticles =
                articleRepository
                        .findNearestArticleIdsWithSimilarity(
                                pgVector,
                                article.getId(),
                                30
                        );

        // =====================================================
        // 8. FIND BEST EVENT
        // =====================================================

        NewsEvent bestEvent = null;

        double bestProbability = -1.0;

        for (Object[] row : nearestArticles) {

            Long candidateArticleId =
                    ((Number) row[0]).longValue();

            double similarity =
                    ((Number) row[1]).doubleValue();

            Article candidateArticle =
                    articleRepository
                            .findById(candidateArticleId)
                            .orElse(null);

            if (candidateArticle == null) {
                continue;
            }

            NewsEvent candidateEvent =
                    candidateArticle.getNewsEvent();

            if (candidateEvent == null) {
                continue;
            }

            // =================================================
            // TEMPORAL SCORE
            // =================================================

            double temporalScore =
                    calculateTemporalScore(
                            article.getPublishedAt(),
                            candidateArticle.getPublishedAt()
                    );

            // =================================================
            // AI EVENT MATCHING
            // =================================================

            EventMatchResponse response =
                    eventMatchingClient.predict(
                            new EventMatchRequest(
                                    similarity,
                                    temporalScore
                            )
                    );

            double probability =
                    response.probability();

            System.out.println(
                    "Candidate Event: "
                            + candidateEvent.getId()
                            + " | Similarity: "
                            + similarity
                            + " | Temporal: "
                            + temporalScore
                            + " | Probability: "
                            + probability
            );

            // =================================================
            // KEEP HIGHEST PROBABILITY
            // =================================================

            if (probability > bestProbability) {

                bestProbability =
                        probability;

                bestEvent =
                        candidateEvent;
            }
        }

        // =====================================================
        // 9. EVENT DECISION
        // =====================================================

        NewsEvent newsEvent;

        if (
                bestEvent != null
                        && bestProbability >= eventMatchThreshold
        ) {

            bestEvent.addArticle(article);

            newsEvent =
                    bestEvent;

            System.out.println(
                    "MATCHED EXISTING EVENT: "
                            + newsEvent.getId()
                            + " | Probability: "
                            + bestProbability
            );

        } else {

            newsEvent =
                    newsEventService.createEvent(
                            article
                    );

            System.out.println(
                    "CREATED NEW EVENT: "
                            + newsEvent.getId()
                            + " | Best probability: "
                            + bestProbability
            );
        }

        // =====================================================
        // 10. MARK PROCESSED
        // =====================================================

        article.setProcessed(true);

        articleRepository.save(article);

        System.out.println(
                "Processed article: "
                        + article.getTitle()
                        + " | Event: "
                        + newsEvent.getId()
                        + " | Category: "
                        + article.getCategory()
                        + " | Sentiment: "
                        + article.getSentiment()
                        + " | Keywords: "
                        + article.getKeywords()
        );
    }

    // =========================================================
    // TEMPORAL SCORE
    // =========================================================

    private double calculateTemporalScore(
            LocalDateTime dateA,
            LocalDateTime dateB
    ) {

        if (dateA == null || dateB == null) {
            return 0.5;
        }

        long days =
                Math.abs(
                        ChronoUnit.DAYS.between(
                                dateA.toLocalDate(),
                                dateB.toLocalDate()
                        )
                );

        if (days == 0) {
            return 1.0;
        }

        if (days <= 1) {
            return 0.9;
        }

        if (days <= 3) {
            return 0.8;
        }

        if (days <= 7) {
            return 0.6;
        }

        if (days <= 30) {
            return 0.4;
        }

        return 0.1;
    }
}