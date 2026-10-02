package com.cfs.xnews.news.articles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ArticleRepository
        extends JpaRepository<Article, Long> {

    boolean existsByUrl(String url);

    Optional<Article> findByUrl(String url);

    List<Article> findByProcessedFalseAndCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    @Query(
            value = """
            SELECT
                a.id,
                1 - (a.embedding <=> CAST(:embedding AS vector)) AS similarity
            FROM articles a
            WHERE a.embedding IS NOT NULL
              AND a.id <> :articleId
            ORDER BY a.embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """,
            nativeQuery = true
    )
    List<Object[]> findNearestArticleIdsWithSimilarity(
            @Param("embedding") String embedding,
            @Param("articleId") Long articleId,
            @Param("limit") int limit
    );

    // v2 matcher: per candidate event, cosine similarity of the new article
    // to the event's members - max, min, mean of the top 3, and the newest
    // member (highest id). Rows: event_id, max, min, top3, newest.
    @Query(
            value = """
            WITH m AS (
                SELECT
                    a.news_event_id AS event_id,
                    1 - (a.embedding <=> CAST(:embedding AS vector)) AS sim,
                    ROW_NUMBER() OVER (
                        PARTITION BY a.news_event_id
                        ORDER BY a.embedding <=> CAST(:embedding AS vector)
                    ) AS sim_rank,
                    ROW_NUMBER() OVER (
                        PARTITION BY a.news_event_id
                        ORDER BY a.id DESC
                    ) AS recency
                FROM articles a
                WHERE a.news_event_id IN (:eventIds)
                  AND a.embedding IS NOT NULL
            )
            SELECT
                event_id,
                MAX(sim),
                MIN(sim),
                AVG(sim) FILTER (WHERE sim_rank <= 3),
                MAX(sim) FILTER (WHERE recency = 1)
            FROM m
            GROUP BY event_id
            """,
            nativeQuery = true
    )
    List<Object[]> findMemberSimilarities(
            @Param("embedding") String embedding,
            @Param("eventIds") List<Long> eventIds
    );

    // v2 matcher: the newest :limit members' texts per candidate event,
    // newest first. Rows: event_id, title, description.
    @Query(
            value = """
            SELECT event_id, title, description
            FROM (
                SELECT
                    a.news_event_id AS event_id,
                    a.id,
                    a.title,
                    a.description,
                    ROW_NUMBER() OVER (
                        PARTITION BY a.news_event_id
                        ORDER BY a.id DESC
                    ) AS recency
                FROM articles a
                WHERE a.news_event_id IN (:eventIds)
            ) newest
            WHERE recency <= :limit
            ORDER BY event_id, recency
            """,
            nativeQuery = true
    )
    List<Object[]> findNewestMemberTexts(
            @Param("eventIds") List<Long> eventIds,
            @Param("limit") int limit
    );

    // v2 vocabulary refit: texts of recently collected articles, newest
    // first. Rows: title, description.
    @Query(
            value = """
            SELECT a.title, a.description
            FROM articles a
            WHERE a.created_at >= :since
            ORDER BY a.id DESC
            LIMIT :limit
            """,
            nativeQuery = true
    )
    List<Object[]> findRecentTexts(
            @Param("since") LocalDateTime since,
            @Param("limit") int limit
    );
}