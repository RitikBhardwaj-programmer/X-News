package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EventSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NewsEventRepository
        extends JpaRepository<NewsEvent, Long> {

    @Query("""
        SELECT
            e.id AS id,
            e.title AS title,
            e.description AS description,
            e.summary AS summary,
            e.createdAt AS createdAt,
            COUNT(DISTINCT a.source) AS sourceCount
        FROM NewsEvent e
        LEFT JOIN e.articles a
        GROUP BY
            e.id,
            e.title,
            e.description,
            e.summary,
            e.createdAt
        ORDER BY e.createdAt DESC
        """)
    List<EventSummaryProjection> findAllEventSummaries();
}