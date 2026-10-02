package com.cfs.xnews.event;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * One event-match decision by one matcher (V4 stage 1b). In shadow mode an
 * article gets two rows: v1 (applied) and v2 (not applied).
 */
@Entity
@Table(name = "event_match_decisions")
public class EventMatchDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Column(nullable = false, length = 10)
    private String matcher;

    @Column(nullable = false, length = 10)
    private String mode;

    @Column(name = "model_version", length = 100)
    private String modelVersion;

    @Column(name = "chosen_event_id")
    private Long chosenEventId;

    @Column(nullable = false)
    private boolean applied;

    private Double probability;

    private Double threshold;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String candidates;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(length = 500)
    private String error;

    // Provenance: the matcher configuration that decided (extraction_runs.id).
    @Column(name = "run_id")
    private Long runId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected EventMatchDecision() {
    }

    public EventMatchDecision(
            Long articleId,
            String matcher,
            String mode,
            String modelVersion,
            Long chosenEventId,
            boolean applied,
            Double probability,
            Double threshold,
            String candidates,
            Integer latencyMs,
            String error
    ) {
        this.articleId = articleId;
        this.matcher = matcher;
        this.mode = mode;
        this.modelVersion = modelVersion;
        this.chosenEventId = chosenEventId;
        this.applied = applied;
        this.probability = probability;
        this.threshold = threshold;
        this.candidates = candidates;
        this.latencyMs = latencyMs;
        this.error = error == null || error.length() <= 500 ? error : error.substring(0, 500);
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getArticleId() { return articleId; }
    public String getMatcher() { return matcher; }
    public String getMode() { return mode; }
    public String getModelVersion() { return modelVersion; }
    public Long getChosenEventId() { return chosenEventId; }
    public boolean isApplied() { return applied; }
    public Double getProbability() { return probability; }
    public Double getThreshold() { return threshold; }
    public String getCandidates() { return candidates; }
    public Integer getLatencyMs() { return latencyMs; }
    public String getError() { return error; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getRunId() { return runId; }
    public void setRunId(Long runId) { this.runId = runId; }
}
