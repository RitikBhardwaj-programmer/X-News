package com.cfs.xnews.processing;

import com.cfs.xnews.event.CentroidUpdateStrategy;
import com.cfs.xnews.event.EventMatchDecision;
import com.cfs.xnews.event.EventMatchDecisionRepository;
import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventRepository;
import com.cfs.xnews.event.NewsEventService;
import com.cfs.xnews.event.dto.EmbeddingResponse;
import com.cfs.xnews.event.dto.EventCandidate;
import com.cfs.xnews.event.dto.EventMatchRequest;
import com.cfs.xnews.event.dto.EventMatchResponse;
import com.cfs.xnews.event.dto.EventMatchResult;
import com.cfs.xnews.kafka.ArticleEvent;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.processing.processor.CategoryProcessor;
import com.cfs.xnews.processing.processor.ContentCleaner;
import com.cfs.xnews.processing.processor.KeywordProcessor;
import com.cfs.xnews.processing.processor.SentimentProcessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Service
public class ArticleProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ArticleProcessingService.class);

    private final NewsEventService newsEventService;
    private final NewsEventRepository newsEventRepository;
    private final SentimentProcessor sentimentProcessor;
    private final CategoryProcessor categoryProcessor;
    private final KeywordProcessor keywordProcessor;
    private final ArticleRepository articleRepository;
    private final ContentCleaner contentCleaner;
    private final EventMatchingClient eventMatchingClient;
    private final CentroidUpdateStrategy centroidUpdateStrategy;

    private final EventMatcherV2 eventMatcherV2;
    private final EventMatchDecisionRepository eventMatchDecisionRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final double eventMatchThreshold;
    private final int candidateLimit;
    private final MatcherMode matcherMode;

    public ArticleProcessingService(
            NewsEventService newsEventService,
            NewsEventRepository newsEventRepository,
            SentimentProcessor sentimentProcessor,
            CategoryProcessor categoryProcessor,
            KeywordProcessor keywordProcessor,
            ArticleRepository articleRepository,
            ContentCleaner contentCleaner,
            EventMatchingClient eventMatchingClient,
            CentroidUpdateStrategy centroidUpdateStrategy,
            EventMatcherV2 eventMatcherV2,
            EventMatchDecisionRepository eventMatchDecisionRepository,

            @Value("${ai.event-matcher.threshold:0.94}")
            double eventMatchThreshold,

            @Value("${ai.event-matcher.candidate-limit:30}")
            int candidateLimit,

            @Value("${ai.event-matcher.mode:v1}")
            String matcherMode
    ) {

        this.newsEventService = newsEventService;
        this.newsEventRepository = newsEventRepository;
        this.sentimentProcessor = sentimentProcessor;
        this.categoryProcessor = categoryProcessor;
        this.keywordProcessor = keywordProcessor;
        this.articleRepository = articleRepository;
        this.contentCleaner = contentCleaner;
        this.eventMatchingClient = eventMatchingClient;
        this.centroidUpdateStrategy = centroidUpdateStrategy;
        this.eventMatcherV2 = eventMatcherV2;
        this.eventMatchDecisionRepository = eventMatchDecisionRepository;
        this.eventMatchThreshold = eventMatchThreshold;
        this.candidateLimit = candidateLimit;
        this.matcherMode = MatcherMode.parse(matcherMode);

        log.info("Event matcher mode: {}", this.matcherMode);
    }

    @Transactional
    public void process(ArticleEvent event) {

        long startedAt = System.nanoTime();

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

            log.info(
                    "Skipping already processed article: {}",
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

        long embedStartedAt = System.nanoTime();

        EmbeddingResponse embeddingResponse =
                eventMatchingClient.generateEmbedding(
                        embeddingText
                );

        long embedMs = elapsedMs(embedStartedAt);

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
        // 7. MATCH AGAINST OPEN EVENT CENTROIDS, OR CREATE
        // =====================================================

        NewsEvent newsEvent = matchOrCreateEvent(
                article,
                embedding,
                embedMs
        );

        // =====================================================
        // 8. MARK PROCESSED
        // =====================================================

        article.setProcessed(true);

        articleRepository.save(article);

        log.info(
                "Processed article={} event={} category={} sentiment={} totalMs={}",
                article.getId(),
                newsEvent.getId(),
                article.getCategory(),
                article.getSentiment(),
                elapsedMs(startedAt)
        );
    }

    // =========================================================
    // EVENT MATCHING
    // =========================================================

    NewsEvent matchOrCreateEvent(
            Article article,
            float[] embedding,
            long embedMs
    ) {

        long retrievalStartedAt = System.nanoTime();

        String pgVector = toPgVector(embedding);

        List<NewsEvent> candidates =
                newsEventRepository.findNearestOpenEvents(
                        pgVector,
                        candidateLimit
                );

        long retrievalMs = elapsedMs(retrievalStartedAt);

        Map<Long, NewsEvent> candidatesById = new HashMap<>();

        for (NewsEvent candidate : candidates) {
            candidatesById.put(candidate.getId(), candidate);
        }

        // v2 runs in shadow and live mode. Only the AI call is caught: a
        // failed SQL statement would abort the article's transaction anyway.
        EventMatcherV2.Outcome v2 = null;
        String v2Error = null;

        if (matcherMode != MatcherMode.V1) {

            try {

                v2 = eventMatcherV2.decide(article, embedding, pgVector, candidates);

            } catch (RestClientException e) {

                v2Error = e.getClass().getSimpleName() + ": " + e.getMessage();

                log.warn(
                        "v2 event matcher failed for article={}, v1 decides: {}",
                        article.getId(),
                        v2Error
                );
            }
        }

        boolean v2Decides = matcherMode == MatcherMode.V2 && v2 != null;

        V1Decision v1 = v2Decides
                ? null
                : decideV1(article, embedding, candidates, candidatesById);

        NewsEvent bestEvent;
        double bestProbability;
        double threshold;

        if (v2Decides) {

            bestEvent = v2.chosenEventId() == null ? null : candidatesById.get(v2.chosenEventId());
            bestProbability = v2.bestProbability() == null ? -1.0 : v2.bestProbability();
            threshold = v2.threshold();

        } else {

            bestEvent = v1.bestEvent();
            bestProbability = v1.bestProbability();
            threshold = eventMatchThreshold;
        }

        boolean matched =
                bestEvent != null
                        && bestProbability >= threshold;

        NewsEvent newsEvent;

        if (matched) {

            // Must run before addArticle(), which increments memberCount.
            bestEvent.setCentroidEmbedding(
                    centroidUpdateStrategy.update(
                            bestEvent.getCentroidEmbedding(),
                            bestEvent.getMemberCount(),
                            embedding
                    )
            );

            bestEvent.addArticle(article);

            newsEvent = bestEvent;

        } else {

            newsEvent = newsEventService.createEvent(article);

            newsEvent.setCentroidEmbedding(
                    centroidUpdateStrategy.update(
                            null,
                            0,
                            embedding
                    )
            );
        }

        if (matcherMode != MatcherMode.V1) {
            recordDecisions(article, v1, v2, v2Error, v2Decides);
        }

        log.info(
                "Event match article={} decision={} event={} matcher={} candidates={} bestProbability={} probabilityBucket={} embedMs={} retrievalMs={} predictMs={} v2Event={} v2Probability={} v2Ms={}",
                article.getId(),
                matched ? "ATTACHED" : "CREATED",
                newsEvent.getId(),
                v2Decides ? "v2" : "v1",
                candidates.size(),
                bestProbability,
                probabilityBucket(bestProbability, threshold),
                embedMs,
                retrievalMs,
                v1 == null ? 0 : v1.predictMs(),
                v2 == null ? null : v2.chosenEventId(),
                v2 == null ? null : v2.bestProbability(),
                v2 == null ? null : v2.latencyMs()
        );

        return newsEvent;
    }

    // v1: the centroid matcher's best candidate (null if there were none).
    private record V1Decision(
            NewsEvent bestEvent,
            double bestProbability,
            long predictMs,
            List<EventMatchResult> results
    ) {
    }

    private V1Decision decideV1(
            Article article,
            float[] embedding,
            List<NewsEvent> candidates,
            Map<Long, NewsEvent> candidatesById
    ) {

        NewsEvent bestEvent = null;
        double bestProbability = -1.0;
        long predictMs = 0;
        List<EventMatchResult> results = List.of();

        if (!candidates.isEmpty()) {

            List<EventCandidate> eventCandidates =
                    candidates.stream()
                            .map(candidate -> new EventCandidate(
                                    candidate.getId(),
                                    candidate.getCentroidEmbedding(),
                                    calculateTemporalScore(
                                            article.getPublishedAt(),
                                            candidate.getLastActivityAt()
                                    )
                            ))
                            .toList();

            long predictStartedAt = System.nanoTime();

            EventMatchResponse response =
                    eventMatchingClient.predict(
                            new EventMatchRequest(
                                    embedding,
                                    eventCandidates
                            )
                    );

            predictMs = elapsedMs(predictStartedAt);

            results = response.results();

            for (EventMatchResult result : results) {

                NewsEvent candidate =
                        candidatesById.get(result.eventId());

                if (candidate == null) {
                    continue;
                }

                log.debug(
                        "Candidate event={} similarity={} probability={}",
                        result.eventId(),
                        result.similarity(),
                        result.probability()
                );

                if (result.probability() > bestProbability) {

                    bestProbability = result.probability();
                    bestEvent = candidate;
                }
            }
        }

        return new V1Decision(bestEvent, bestProbability, predictMs, results);
    }

    // =========================================================
    // DECISION RECORDS (shadow and live mode)
    // =========================================================

    private void recordDecisions(
            Article article,
            V1Decision v1,
            EventMatcherV2.Outcome v2,
            String v2Error,
            boolean v2Decides
    ) {

        String mode = matcherMode.decisionMode();

        if (v1 != null) {

            boolean v1Attaches =
                    v1.bestEvent() != null
                            && v1.bestProbability() >= eventMatchThreshold;

            eventMatchDecisionRepository.save(new EventMatchDecision(
                    article.getId(),
                    "v1",
                    mode,
                    "v1-centroid",
                    v1Attaches ? v1.bestEvent().getId() : null,
                    true,
                    v1.bestProbability() < 0 ? null : v1.bestProbability(),
                    eventMatchThreshold,
                    v1CandidatesJson(v1.results()),
                    (int) v1.predictMs(),
                    null
            ));
        }

        if (v2 != null) {

            eventMatchDecisionRepository.save(new EventMatchDecision(
                    article.getId(),
                    "v2",
                    mode,
                    v2.modelVersion(),
                    v2.chosenEventId(),
                    v2Decides,
                    v2.bestProbability(),
                    v2.threshold(),
                    v2.candidatesJson(),
                    v2.latencyMs(),
                    null
            ));

        } else if (v2Error != null) {

            eventMatchDecisionRepository.save(new EventMatchDecision(
                    article.getId(),
                    "v2",
                    mode,
                    null,
                    null,
                    false,
                    null,
                    null,
                    null,
                    null,
                    v2Error
            ));
        }
    }

    private String v1CandidatesJson(List<EventMatchResult> results) {

        List<Map<String, Object>> top = results.stream()
                .sorted(Comparator.comparingDouble(EventMatchResult::probability).reversed())
                .limit(EventMatcherV2.STORED_CANDIDATES)
                .map(result -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("event_id", result.eventId());
                    entry.put("probability", result.probability());
                    entry.put("similarity", result.similarity());
                    return entry;
                })
                .toList();

        try {
            return objectMapper.writeValueAsString(top);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise v1 candidates", e);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private static String toPgVector(float[] embedding) {

        StringBuilder builder = new StringBuilder("[");

        for (int i = 0; i < embedding.length; i++) {

            if (i > 0) {
                builder.append(",");
            }

            builder.append(embedding[i]);
        }

        return builder.append("]").toString();
    }

    private static long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }

    // Buckets sit relative to the threshold, so they keep showing whether it
    // is behaving (a pile-up just below or above it is the warning sign)
    // however the model's score scale or the threshold changes.
    static String probabilityBucket(double probability, double threshold) {

        if (probability < 0) {
            return "none";
        }

        if (probability < threshold - 0.20) {
            return "far-below";
        }

        if (probability < threshold - 0.05) {
            return "below";
        }

        if (probability < threshold) {
            return "just-below";
        }

        if (probability < threshold + 0.05) {
            return "just-above";
        }

        return "well-above";
    }

    // =========================================================
    // TEMPORAL SCORE (article date vs. the event's last activity)
    // =========================================================

    static double calculateTemporalScore(
            LocalDateTime articleDate,
            LocalDateTime eventLastActivity
    ) {

        if (articleDate == null || eventLastActivity == null) {
            return 0.5;
        }

        long days =
                Math.abs(
                        ChronoUnit.DAYS.between(
                                articleDate.toLocalDate(),
                                eventLastActivity.toLocalDate()
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
