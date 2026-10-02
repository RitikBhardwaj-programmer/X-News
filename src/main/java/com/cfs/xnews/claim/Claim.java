package com.cfs.xnews.claim;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One article's statement of a number about a subject (V4 roadmap step 6),
 * with the exact quote it rests on. Append-only: never edited after insert.
 */
@Entity
@Table(name = "claims")
public class Claim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(name = "event_id")
    private Long eventId;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(name = "subject_normalized", nullable = false, length = 200)
    private String subjectNormalized;

    @Column(nullable = false, length = 30)
    private String predicate;

    @Column(nullable = false)
    private BigDecimal value;

    @Column(name = "value_text", nullable = false, length = 30)
    private String valueText;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false, length = 500)
    private String quote;

    @Column(name = "quote_start", nullable = false)
    private int quoteStart;

    @Column(name = "quote_end", nullable = false)
    private int quoteEnd;

    @Column(nullable = false, length = 15)
    private String field;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Claim() {
    }

    public Claim(Long articleId, Long eventId, String subject, String subjectNormalized, String predicate,
                 BigDecimal value, String valueText, String unit, String quote, int quoteStart, int quoteEnd,
                 String field, Long runId) {
        this.articleId = articleId;
        this.eventId = eventId;
        this.subject = subject;
        this.subjectNormalized = subjectNormalized;
        this.predicate = predicate;
        this.value = value;
        this.valueText = valueText;
        this.unit = unit;
        this.quote = quote;
        this.quoteStart = quoteStart;
        this.quoteEnd = quoteEnd;
        this.field = field;
        this.runId = runId;
    }

    public Long getId() { return id; }
    public Long getArticleId() { return articleId; }
    public Long getEventId() { return eventId; }
    public String getSubjectNormalized() { return subjectNormalized; }
    public String getPredicate() { return predicate; }
    public String getValueText() { return valueText; }
    public String getUnit() { return unit; }
}
