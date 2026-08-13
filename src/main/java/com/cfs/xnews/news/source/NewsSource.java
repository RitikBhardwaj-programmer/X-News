package com.cfs.xnews.news.source;



import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "news_sources",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_news_source_url",
                        columnNames = "url"
                )
        }
)
public class NewsSource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NewsSourceType type;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected NewsSource() {
    }

    public NewsSource(
            String name,
            String url,
            NewsSourceType type
    ) {
        this.name = name;
        this.url = url;
        this.type = type;
        this.enabled = true;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public NewsSourceType getType() {
        return type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
