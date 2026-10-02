package com.cfs.xnews.processing;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.dto.ArticleText;
import com.cfs.xnews.event.dto.EventCandidateV2;
import com.cfs.xnews.event.dto.EventMatchV2Request;
import com.cfs.xnews.event.dto.EventMatchV2Response;
import com.cfs.xnews.event.dto.EventMatchV2Result;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The v2 event matcher (variant B, V4 stage 1b): member similarities and the
 * newest members' texts come from SQL, the AI service's /predict/v2 adds the
 * TF-IDF wording features and scores, and the best candidate attaches if its
 * probability reaches the model's threshold.
 */
@Component
public class EventMatcherV2 {

    static final int MEMBER_TEXT_CAP = 20;
    static final int MAX_TITLE_CHARS = 500;
    static final int MAX_DESCRIPTION_CHARS = 5000;
    static final int STORED_CANDIDATES = 5;
    static final String DEFAULT_VOCABULARY = "default";

    private final ArticleRepository articleRepository;
    private final EventMatchingClient eventMatchingClient;
    private final VocabularyRefitScheduler vocabularyRefitScheduler;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventMatcherV2(
            ArticleRepository articleRepository,
            EventMatchingClient eventMatchingClient,
            VocabularyRefitScheduler vocabularyRefitScheduler
    ) {
        this.articleRepository = articleRepository;
        this.eventMatchingClient = eventMatchingClient;
        this.vocabularyRefitScheduler = vocabularyRefitScheduler;
    }

    /**
     * chosenEventId is null when no candidate reaches the threshold (a new
     * event). bestProbability is null when there were no candidates.
     */
    public record Outcome(
            Long chosenEventId,
            Double bestProbability,
            double threshold,
            String modelVersion,
            String candidatesJson,
            int latencyMs
    ) {
    }

    /**
     * Throws RestClientException (including timeouts) when the AI service
     * call fails; the caller decides what that means for the mode.
     */
    public Outcome decide(
            Article article,
            float[] embedding,
            String pgVector,
            List<NewsEvent> candidates
    ) {

        long startedAt = System.nanoTime();

        List<EventCandidateV2> requestCandidates =
                candidates.isEmpty()
                        ? List.of()
                        : buildCandidates(article, embedding, pgVector, candidates);

        EventMatchV2Response response =
                eventMatchingClient.predictV2(
                        new EventMatchV2Request(
                                truncate(article.getTitle(), MAX_TITLE_CHARS),
                                truncate(article.getDescription(), MAX_DESCRIPTION_CHARS),
                                requestCandidates
                        )
                );

        // An empty body counts as a failed call, so live mode falls back to v1.
        if (response == null || response.results() == null) {
            throw new RestClientException("Empty /predict/v2 response");
        }

        if (DEFAULT_VOCABULARY.equals(response.vocabularyVersion())) {
            // The AI service restarted and lost its refitted vocabulary.
            vocabularyRefitScheduler.requestRefit();
        }

        EventMatchV2Result best = best(response.results());

        return new Outcome(
                attaches(best, response.threshold()) ? best.eventId() : null,
                best == null ? null : best.probability(),
                response.threshold(),
                response.modelVersion() + "/" + response.vocabularyVersion(),
                candidatesJson(response.results()),
                (int) ((System.nanoTime() - startedAt) / 1_000_000)
        );
    }

    private List<EventCandidateV2> buildCandidates(
            Article article,
            float[] embedding,
            String pgVector,
            List<NewsEvent> candidates
    ) {

        List<Long> eventIds = candidates.stream().map(NewsEvent::getId).toList();

        Map<Long, Object[]> similarities = new HashMap<>();
        for (Object[] row : articleRepository.findMemberSimilarities(pgVector, eventIds)) {
            similarities.put(((Number) row[0]).longValue(), row);
        }

        Map<Long, List<ArticleText>> texts = new HashMap<>();
        for (Object[] row : articleRepository.findNewestMemberTexts(eventIds, MEMBER_TEXT_CAP)) {
            texts.computeIfAbsent(((Number) row[0]).longValue(), id -> new ArrayList<>())
                    .add(new ArticleText(
                            truncate((String) row[1], MAX_TITLE_CHARS),
                            truncate((String) row[2], MAX_DESCRIPTION_CHARS)
                    ));
        }

        List<EventCandidateV2> result = new ArrayList<>();

        for (NewsEvent candidate : candidates) {

            Object[] row = similarities.get(candidate.getId());
            List<ArticleText> members = texts.get(candidate.getId());

            // An event without embedded members can't be scored by v2.
            if (row == null || members == null) {
                continue;
            }

            result.add(new EventCandidateV2(
                    candidate.getId(),
                    cosine(candidate.getCentroidEmbedding(), embedding),
                    ArticleProcessingService.calculateTemporalScore(
                            article.getPublishedAt(),
                            candidate.getLastActivityAt()
                    ),
                    similarity(row[1]),
                    similarity(row[2]),
                    similarity(row[3]),
                    similarity(row[4]),
                    members
            ));
        }

        return result;
    }

    // =========================================================
    // DECISION (pure, unit tested)
    // =========================================================

    // Highest probability; the first candidate wins a tie, as in the
    // offline evaluation (numpy argmax).
    static EventMatchV2Result best(List<EventMatchV2Result> results) {

        EventMatchV2Result best = null;

        for (EventMatchV2Result result : results) {
            if (best == null || result.probability() > best.probability()) {
                best = result;
            }
        }

        return best;
    }

    static boolean attaches(EventMatchV2Result best, double threshold) {
        return best != null && best.probability() >= threshold;
    }

    static double cosine(float[] a, float[] b) {

        double dot = 0, normA = 0, normB = 0;

        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        return normA == 0 || normB == 0 ? 0 : clamp(dot / Math.sqrt(normA * normB));
    }

    // A duplicate article's cosine can land a rounding error above 1
    // (e.g. 1.0000000000000016), which the AI service rejects with 422.
    static double clamp(double similarity) {
        return Math.max(-1.0, Math.min(1.0, similarity));
    }

    private static double similarity(Object sqlValue) {
        return clamp(((Number) sqlValue).doubleValue());
    }

    static String truncate(String text, int maxChars) {
        return text == null || text.length() <= maxChars ? text : text.substring(0, maxChars);
    }

    private String candidatesJson(List<EventMatchV2Result> results) {

        List<Map<String, Object>> top = results.stream()
                .sorted(Comparator.comparingDouble(EventMatchV2Result::probability).reversed())
                .limit(STORED_CANDIDATES)
                .map(result -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("event_id", result.eventId());
                    entry.put("probability", result.probability());
                    entry.put("features", result.features());
                    return entry;
                })
                .toList();

        try {
            return objectMapper.writeValueAsString(top);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise v2 candidates", e);
        }
    }
}
