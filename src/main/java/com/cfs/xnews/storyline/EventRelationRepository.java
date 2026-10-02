package com.cfs.xnews.storyline;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRelationRepository
        extends JpaRepository<EventRelation, Long> {

    /**
     * Links recent events that share at least :minShared distinctive
     * entities and whose centroids are at least :minCosine similar.
     * An entity is not distinctive when more than max(:minGeneric, 2% of the
     * recent events) mention it ("India", "PM Modi"). Existing pairs are
     * updated, so a pair can move from related to followed_by as events end.
     */
    @Modifying
    @Query(
            value = """
            WITH recent AS (
                SELECT id, centroid_embedding, first_activity_at, last_activity_at
                FROM news_events
                WHERE last_activity_at >= :since
                  AND centroid_embedding IS NOT NULL
            ),
            event_entities AS (
                SELECT DISTINCT a.news_event_id AS event_id, m.entity_id
                FROM entity_mentions m
                JOIN articles a ON a.id = m.article_id
                WHERE a.news_event_id IN (SELECT id FROM recent)
            ),
            generic AS (
                SELECT entity_id
                FROM event_entities
                GROUP BY entity_id
                HAVING COUNT(*) > GREATEST(:minGeneric,
                    0.02 * (SELECT COUNT(DISTINCT event_id) FROM event_entities))
            ),
            distinctive AS (
                SELECT * FROM event_entities
                WHERE entity_id NOT IN (SELECT entity_id FROM generic)
            ),
            pairs AS (
                SELECT x.event_id AS a, y.event_id AS b, COUNT(*) AS shared
                FROM distinctive x
                JOIN distinctive y ON x.entity_id = y.entity_id AND x.event_id < y.event_id
                GROUP BY x.event_id, y.event_id
                HAVING COUNT(*) >= :minShared
            ),
            scored AS (
                SELECT
                    -- the event that started first is "from"
                    CASE WHEN (ea.first_activity_at, ea.id) <= (eb.first_activity_at, eb.id) THEN ea.id ELSE eb.id END AS from_id,
                    CASE WHEN (ea.first_activity_at, ea.id) <= (eb.first_activity_at, eb.id) THEN eb.id ELSE ea.id END AS to_id,
                    CASE WHEN ea.last_activity_at < eb.first_activity_at
                           OR eb.last_activity_at < ea.first_activity_at
                         THEN 'followed_by' ELSE 'related' END AS relation,
                    1 - (ea.centroid_embedding <=> eb.centroid_embedding) AS score,
                    p.shared
                FROM pairs p
                JOIN recent ea ON ea.id = p.a
                JOIN recent eb ON eb.id = p.b
            )
            INSERT INTO event_relations (from_event_id, to_event_id, relation, score, shared_entities, run_id, updated_at)
            SELECT from_id, to_id, relation, score, shared, :runId, now()
            FROM scored
            WHERE score >= :minCosine
            ON CONFLICT (from_event_id, to_event_id) DO UPDATE SET
                relation = EXCLUDED.relation,
                score = EXCLUDED.score,
                shared_entities = EXCLUDED.shared_entities,
                run_id = EXCLUDED.run_id,
                updated_at = now()
            """,
            nativeQuery = true
    )
    int linkRecentEvents(
            @Param("since") LocalDateTime since,
            @Param("minShared") int minShared,
            @Param("minCosine") double minCosine,
            @Param("minGeneric") int minGeneric,
            @Param("runId") Long runId
    );

    /**
     * The storyline around an event: the event and everything linked to it
     * within :maxDepth hops, oldest first. depth 0 is the event itself.
     * Rows: id, title, first_activity_at, last_activity_at, member_count, depth.
     */
    @Query(
            value = """
            WITH RECURSIVE linked(event_id, depth) AS (
                SELECT CAST(:eventId AS bigint), 0
                UNION
                SELECT CASE WHEN r.from_event_id = l.event_id THEN r.to_event_id ELSE r.from_event_id END,
                       l.depth + 1
                FROM event_relations r
                JOIN linked l ON l.event_id IN (r.from_event_id, r.to_event_id)
                WHERE l.depth < :maxDepth
            )
            SELECT e.id, e.title, e.first_activity_at, e.last_activity_at, e.member_count, MIN(l.depth) AS depth
            FROM linked l
            JOIN news_events e ON e.id = l.event_id
            GROUP BY e.id, e.title, e.first_activity_at, e.last_activity_at, e.member_count
            ORDER BY e.first_activity_at, e.id
            LIMIT :limit
            """,
            nativeQuery = true
    )
    List<Object[]> findStoryline(
            @Param("eventId") Long eventId,
            @Param("maxDepth") int maxDepth,
            @Param("limit") int limit
    );
}
