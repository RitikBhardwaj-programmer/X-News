package com.cfs.xnews.analysis;

import com.cfs.xnews.event.NewsEvent;
import jakarta.persistence.*;

@Entity
@Table(name = "fact_checks")
public class FactCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String agency;

    @Column(columnDefinition = "TEXT")
    private String claim;

    @Column(length = 30)
    private String verdict;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    private String sourceUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency(String agency) {
        this.agency = agency;
    }

    public String getClaim() {
        return claim;
    }

    public void setClaim(String claim) {
        this.claim = claim;
    }

    public String getVerdict() {
        return verdict;
    }

    public void setVerdict(String verdict) {
        this.verdict = verdict;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public NewsEvent getNewsEvent() {
        return newsEvent;
    }

    public void setNewsEvent(NewsEvent newsEvent) {
        this.newsEvent = newsEvent;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "news_event_id")
    private NewsEvent newsEvent;

    public FactCheck() {
    }

    // getters/setters
}