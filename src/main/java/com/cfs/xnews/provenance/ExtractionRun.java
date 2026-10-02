package com.cfs.xnews.provenance;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * The configuration that produced a generated output (V4 roadmap step 1):
 * kind of step, model, prompt version and code version.
 */
@Entity
@Table(name = "extraction_runs")
public class ExtractionRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String kind;

    @Column(nullable = false, length = 150)
    private String model;

    @Column(name = "prompt_version", nullable = false, length = 40)
    private String promptVersion;

    @Column(name = "code_version", nullable = false, length = 40)
    private String codeVersion;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected ExtractionRun() {
    }

    public Long getId() { return id; }
    public String getKind() { return kind; }
    public String getModel() { return model; }
    public String getPromptVersion() { return promptVersion; }
    public String getCodeVersion() { return codeVersion; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
