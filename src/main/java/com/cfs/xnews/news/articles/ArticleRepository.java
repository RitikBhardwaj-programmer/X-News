package com.cfs.xnews.news.articles;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ArticleRepository
        extends JpaRepository<Article, Long> {

    boolean existsByUrl(String url);

    Optional<Article> findByUrl(String url);

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
}