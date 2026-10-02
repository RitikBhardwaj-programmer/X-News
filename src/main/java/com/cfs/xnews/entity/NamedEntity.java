package com.cfs.xnews.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * A person, place, organisation or team that articles mention (V4 roadmap
 * step 3). Spellings map to it through entity_aliases.
 */
@Entity
@Table(name = "entities")
public class NamedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "canonical_name", nullable = false, length = 200)
    private String canonicalName;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected NamedEntity() {
    }

    public NamedEntity(String canonicalName, String type) {
        this.canonicalName = canonicalName;
        this.type = type;
    }

    public Long getId() { return id; }
    public String getCanonicalName() { return canonicalName; }
    public String getType() { return type; }
}
