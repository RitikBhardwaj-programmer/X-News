package com.cfs.xnews.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NamedEntityRepository
        extends JpaRepository<NamedEntity, Long> {

    @Query(
            value = "SELECT entity_id FROM entity_aliases WHERE alias = :alias",
            nativeQuery = true
    )
    Optional<Long> findIdByAlias(@Param("alias") String alias);

    // 0 when another transaction already owns the alias.
    @Modifying
    @Query(
            value = """
            INSERT INTO entity_aliases (entity_id, alias)
            VALUES (:entityId, :alias)
            ON CONFLICT (alias) DO NOTHING
            """,
            nativeQuery = true
    )
    int insertAliasIfAbsent(@Param("entityId") Long entityId, @Param("alias") String alias);

    @Modifying
    @Query(
            value = """
            INSERT INTO entity_mentions (article_id, entity_id, surface, field, run_id)
            VALUES (:articleId, :entityId, :surface, :field, :runId)
            ON CONFLICT (article_id, entity_id, field) DO NOTHING
            """,
            nativeQuery = true
    )
    int insertMentionIfAbsent(
            @Param("articleId") Long articleId,
            @Param("entityId") Long entityId,
            @Param("surface") String surface,
            @Param("field") String field,
            @Param("runId") Long runId
    );

    @Query(
            value = "SELECT EXISTS (SELECT 1 FROM entity_mentions WHERE article_id = :articleId)",
            nativeQuery = true
    )
    boolean hasMentions(@Param("articleId") Long articleId);

    // Entities across an event's articles, most widely reported first.
    // Rows: id, canonical_name, type, article_count.
    @Query(
            value = """
            SELECT e.id, e.canonical_name, e.type, COUNT(DISTINCT m.article_id) AS articles
            FROM entity_mentions m
            JOIN articles a ON a.id = m.article_id
            JOIN entities e ON e.id = m.entity_id
            WHERE a.news_event_id = :eventId
            GROUP BY e.id, e.canonical_name, e.type
            ORDER BY articles DESC, e.canonical_name
            LIMIT :limit
            """,
            nativeQuery = true
    )
    List<Object[]> findForEvent(@Param("eventId") Long eventId, @Param("limit") int limit);
}
