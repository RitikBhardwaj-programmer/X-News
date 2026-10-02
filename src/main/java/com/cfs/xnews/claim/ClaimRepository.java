package com.cfs.xnews.claim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    @Query(
            value = "SELECT EXISTS (SELECT 1 FROM claims WHERE article_id = :articleId)",
            nativeQuery = true
    )
    boolean hasClaims(@Param("articleId") Long articleId);

    // Other articles' claims in the same event about the same subject,
    // predicate and unit: evidence for or against a new claim.
    @Query("""
        SELECT c FROM Claim c
        WHERE c.eventId = :eventId
          AND c.subjectNormalized = :subject
          AND c.predicate = :predicate
          AND c.unit = :unit
          AND c.articleId <> :articleId
        """)
    List<Claim> findComparable(
            @Param("eventId") Long eventId,
            @Param("subject") String subject,
            @Param("predicate") String predicate,
            @Param("unit") String unit,
            @Param("articleId") Long articleId
    );

    @Modifying
    @Query(
            value = """
            INSERT INTO claim_evidence (claim_id, article_id, label)
            VALUES (:claimId, :articleId, :label)
            ON CONFLICT (claim_id, article_id) DO NOTHING
            """,
            nativeQuery = true
    )
    int insertEvidenceIfAbsent(
            @Param("claimId") Long claimId,
            @Param("articleId") Long articleId,
            @Param("label") String label
    );

    // Claims with their latest review status ('pending' without a review)
    // and evidence counts. Evidence counts only articles whose comparable
    // claim hasn't been rejected (a rejected misreading is no evidence).
    // :eventId null = all events.
    // Rows: id, subject, predicate, value_text, unit, quote, field, created_at,
    // article_id, article_title, article_url, source, event_id, status,
    // supports, refutes.
    @Query(
            value = """
            SELECT * FROM (
                SELECT c.id, c.subject, c.predicate, c.value_text, c.unit, c.quote, c.field, c.created_at,
                       a.id AS article_id, a.title, a.url, a.source, c.event_id,
                       COALESCE((SELECT r.decision FROM claim_reviews r
                                 WHERE r.claim_id = c.id ORDER BY r.id DESC LIMIT 1), 'pending') AS status,
                       (SELECT COUNT(*) FROM claim_evidence e
                        WHERE e.claim_id = c.id AND e.label = 'SUPPORTS'
                          AND EXISTS (SELECT 1 FROM claims o
                                      WHERE o.article_id = e.article_id AND o.event_id = c.event_id
                                        AND o.subject_normalized = c.subject_normalized
                                        AND o.predicate = c.predicate AND o.unit = c.unit
                                        AND COALESCE((SELECT r.decision FROM claim_reviews r
                                                      WHERE r.claim_id = o.id ORDER BY r.id DESC LIMIT 1),
                                                     'pending') <> 'rejected')) AS supports,
                       (SELECT COUNT(*) FROM claim_evidence e
                        WHERE e.claim_id = c.id AND e.label = 'REFUTES'
                          AND EXISTS (SELECT 1 FROM claims o
                                      WHERE o.article_id = e.article_id AND o.event_id = c.event_id
                                        AND o.subject_normalized = c.subject_normalized
                                        AND o.predicate = c.predicate AND o.unit = c.unit
                                        AND o.value_text <> c.value_text
                                        AND COALESCE((SELECT r.decision FROM claim_reviews r
                                                      WHERE r.claim_id = o.id ORDER BY r.id DESC LIMIT 1),
                                                     'pending') <> 'rejected')) AS refutes
                FROM claims c
                JOIN articles a ON a.id = c.article_id
                WHERE (CAST(:eventId AS bigint) IS NULL OR c.event_id = :eventId)
            ) listed
            WHERE status = :status
            ORDER BY id DESC
            LIMIT :limit
            """,
            nativeQuery = true
    )
    List<Object[]> findWithStatus(
            @Param("status") String status,
            @Param("eventId") Long eventId,
            @Param("limit") int limit
    );
}
