package com.cfs.xnews.news.articles;


import com.cfs.xnews.event.NewsEvent;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "articles",
        indexes = {
                @Index(name = "idx_article_published_at",
                        columnList = "published_at"),
                @Index(name = "idx_article_source",
                        columnList = "source")
        }
)
public class Article {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_event_id")
    @JsonBackReference
    private NewsEvent newsEvent;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(nullable = false, length = 255)
    private String source;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 50)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String keywords;

    @Column(length = 30)
    private String sentiment;

    protected Article() {
    }

    public Article(
            String title,
            String description,
            String url,
            String source,
            LocalDateTime publishedAt
    ) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.source = source;
        this.publishedAt = publishedAt;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getUrl() {
        return url;
    }

    public String getSource() {
        return source;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public void setDescription(String cleanedDescription) {
        this.description = cleanedDescription;
    }
    public NewsEvent getNewsEvent() {
        return newsEvent;
    }

    public void setNewsEvent(NewsEvent newsEvent) {
        this.newsEvent = newsEvent;
    }
}
