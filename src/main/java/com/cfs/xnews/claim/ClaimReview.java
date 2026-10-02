package com.cfs.xnews.claim;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** One review decision (append-only log); a claim's status is its latest review. */
@Entity
@Table(name = "claim_reviews")
public class ClaimReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "claim_id", nullable = false)
    private Long claimId;

    @Column(nullable = false, length = 10)
    private String decision;

    @Column(nullable = false, length = 255)
    private String reviewer;

    @Column(length = 500)
    private String note;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ClaimReview() {
    }

    public ClaimReview(Long claimId, String decision, String reviewer, String note) {
        this.claimId = claimId;
        this.decision = decision;
        this.reviewer = reviewer;
        this.note = note;
    }

    public Long getId() { return id; }
    public Long getClaimId() { return claimId; }
    public String getDecision() { return decision; }
    public String getReviewer() { return reviewer; }
    public String getNote() { return note; }
}
