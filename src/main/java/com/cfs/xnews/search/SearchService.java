package com.cfs.xnews.search;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.dto.EmbeddingResponse;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hybrid article search (V4 roadmap step 5): Postgres full text fused with
 * the stored MiniLM embeddings. Falls back to full text alone when the AI
 * service can't embed the query.
 */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    static final int MAX_QUERY_CHARS = 200;
    static final int MAX_LIMIT = 50;
    static final int EMBEDDING_DIMENSIONS = 384;

    private final ArticleRepository articleRepository;
    private final EventMatchingClient eventMatchingClient;

    public SearchService(
            ArticleRepository articleRepository,
            EventMatchingClient eventMatchingClient
    ) {
        this.articleRepository = articleRepository;
        this.eventMatchingClient = eventMatchingClient;
    }

    public SearchResponse search(String query, int limit) {

        String text = query == null ? "" : query.strip();

        // IllegalArgumentException is a RuntimeException: the global handler
        // answers 400 with the message.
        if (text.isEmpty()) {
            throw new IllegalArgumentException("Search query is required");
        }

        if (text.length() > MAX_QUERY_CHARS) {
            throw new IllegalArgumentException(
                    "Search query must be at most " + MAX_QUERY_CHARS + " characters"
            );
        }

        int size = Math.max(1, Math.min(limit, MAX_LIMIT));
        String embedding = embed(text);

        List<Object[]> rows = embedding == null
                ? articleRepository.searchText(text, size)
                : articleRepository.searchHybrid(text, embedding, size);

        return new SearchResponse(
                text,
                embedding == null ? "text" : "hybrid",
                rows.stream().map(SearchService::toResult).toList()
        );
    }

    private String embed(String text) {

        try {

            EmbeddingResponse response = eventMatchingClient.generateEmbeddingWithTimeout(text);

            if (response == null
                    || response.embedding() == null
                    || response.embedding().size() != EMBEDDING_DIMENSIONS) {
                log.warn("Search: unexpected embedding, using full text only");
                return null;
            }

            return response.embedding().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]"));

        } catch (RestClientException e) {

            log.warn("Search: AI service unavailable, using full text only: {}", e.getMessage());
            return null;
        }
    }

    static SearchResponse.Result toResult(Object[] row) {

        return new SearchResponse.Result(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                toLocalDateTime(row[4]),
                row[5] == null ? null : ((Number) row[5]).longValue(),
                ((Number) row[6]).doubleValue()
        );
    }

    // Native queries return java.sql.Timestamp or LocalDateTime depending
    // on the driver and Hibernate version.
    private static LocalDateTime toLocalDateTime(Object value) {

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        return (LocalDateTime) value;
    }
}
