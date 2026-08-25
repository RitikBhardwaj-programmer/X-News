package com.cfs.xnews.event;

import com.cfs.xnews.analysis.FactCheck;
import com.cfs.xnews.news.articles.Article;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "news_events")
public class NewsEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "newsEvent")
    @JsonManagedReference
    private List<Article> articles = new ArrayList<>();

    @OneToMany(mappedBy = "newsEvent")
    private List<FactCheck> factChecks = new ArrayList<>();
    public NewsEvent() {
    }

    public NewsEvent(
            String title,
            String description
    ) {
        this.title = title;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }
    public void addArticle(Article article) {

        articles.add(article);

        article.setNewsEvent(this);
    }

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(length = 30)
    private String disagreementLevel;

    @Column(columnDefinition = "TEXT")
    private String biasAnalysis;

    @Column(length = 30)
    private String verificationStatus;

    private Double misinformationRisk;
    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getDisagreementLevel() {
        return disagreementLevel;
    }

    public void setDisagreementLevel(String disagreementLevel) {
        this.disagreementLevel = disagreementLevel;
    }

    public String getBiasAnalysis() {
        return biasAnalysis;
    }

    public void setBiasAnalysis(String biasAnalysis) {
        this.biasAnalysis = biasAnalysis;
    }

    public String getVerificationStatus() {
        return verificationStatus;
    }

    public void setVerificationStatus(String verificationStatus) {
        this.verificationStatus = verificationStatus;
    }

    public Double getMisinformationRisk() {
        return misinformationRisk;
    }

    public void setMisinformationRisk(Double misinformationRisk) {
        this.misinformationRisk = misinformationRisk;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<Article> getArticles() {
        return articles;
    }
}