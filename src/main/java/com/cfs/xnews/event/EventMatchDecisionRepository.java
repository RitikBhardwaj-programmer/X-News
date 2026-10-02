package com.cfs.xnews.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventMatchDecisionRepository
        extends JpaRepository<EventMatchDecision, Long> {

    // The decision that was actually applied for each article now in the
    // event (one per article; none for articles from before decisions were
    // recorded).
    @Query("""
        SELECT d
        FROM EventMatchDecision d
        WHERE d.applied = true
          AND d.articleId IN (
              SELECT a.id FROM Article a WHERE a.newsEvent.id = :eventId
          )
        ORDER BY d.createdAt
        """)
    List<EventMatchDecision> findAppliedForEvent(@Param("eventId") Long eventId);
}
