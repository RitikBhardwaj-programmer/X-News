package com.cfs.xnews.storyline;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A link between two events of one storyline (V4 roadmap step 7).
 * fromEventId started first; relation is "followed_by" when it ended before
 * the other began, otherwise "related".
 */
@Entity
@Table(name = "event_relations")
public class EventRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "from_event_id", nullable = false)
    private Long fromEventId;

    @Column(name = "to_event_id", nullable = false)
    private Long toEventId;

    @Column(nullable = false, length = 20)
    private String relation;

    @Column(nullable = false)
    private double score;

    @Column(name = "shared_entities", nullable = false)
    private int sharedEntities;

    @Column(name = "run_id")
    private Long runId;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected EventRelation() {
    }

    public Long getId() { return id; }
    public Long getFromEventId() { return fromEventId; }
    public Long getToEventId() { return toEventId; }
    public String getRelation() { return relation; }
    public double getScore() { return score; }
    public int getSharedEntities() { return sharedEntities; }
    public Long getRunId() { return runId; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
