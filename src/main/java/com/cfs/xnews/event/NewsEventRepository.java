package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EventSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsEventRepository
        extends JpaRepository<NewsEvent, Long> {

    // Only OPEN events are searched, which also keeps this fast as the
    // number of historical events grows.
    @Query(
            value = """
            SELECT e.*
            FROM news_events e
            WHERE e.status = 'OPEN'
              AND e.centroid_embedding IS NOT NULL
            ORDER BY e.centroid_embedding <=> CAST(:embedding AS vector)
            LIMIT :limit
            """,
            nativeQuery = true
    )
    List<NewsEvent> findNearestOpenEvents(
            @Param("embedding") String embedding,
            @Param("limit") int limit
    );

    @Modifying
    @Query("""
        UPDATE NewsEvent e
        SET e.status = :closed
        WHERE e.status = :open
          AND e.lastActivityAt < :cutoff
        """)
    int closeEventsInactiveSince(
            @Param("open") EventStatus open,
            @Param("closed") EventStatus closed,
            @Param("cutoff") LocalDateTime cutoff
    );

    @Query("""
        SELECT
            e.id AS id,
            e.title AS title,
            e.description AS description,
            e.summary AS summary,
            e.createdAt AS createdAt,
            e.verificationStatus AS verificationStatus,
            e.disagreementLevel AS disagreementLevel,
            e.misinformationRisk AS misinformationRisk,
            COUNT(a) AS sourceCount
        FROM NewsEvent e
        LEFT JOIN e.articles a
        GROUP BY
            e.id,
            e.title,
            e.description,
            e.summary,
            e.createdAt,
            e.verificationStatus,
            e.disagreementLevel,
            e.misinformationRisk
        ORDER BY e.createdAt DESC
        """)
    List<EventSummaryProjection> findAllEventSummaries();
}