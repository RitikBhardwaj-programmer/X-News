package com.cfs.xnews.claim;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.dto.ClaimExtractionResponse;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.provenance.ExtractionRunService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClaimServiceTest {

    private ClaimRepository claims;
    private ClaimReviewRepository reviews;
    private ArticleRepository articles;
    private EventMatchingClient client;
    private ExtractionRunService runs;
    private ClaimService service;

    @BeforeEach
    void setUp() {
        claims = mock(ClaimRepository.class);
        reviews = mock(ClaimReviewRepository.class);
        articles = mock(ArticleRepository.class);
        client = mock(EventMatchingClient.class);
        runs = mock(ExtractionRunService.class);
        service = new ClaimService(claims, reviews, articles, client, runs);
        when(runs.runId(any(), any(), any())).thenReturn(4L);
    }

    private static ClaimExtractionResponse.Claim extracted(String predicate, String valueText, String field) {
        return new ClaimExtractionResponse.Claim("Virat Kohli", "virat kohli", predicate, 139, valueText, "runs",
                "Virat Kohli's 139", 0, 17, field);
    }

    private Article articleInEvent(long eventId) {
        Article article = mock(Article.class);
        NewsEvent event = mock(NewsEvent.class);
        when(event.getId()).thenReturn(eventId);
        when(article.getNewsEvent()).thenReturn(event);
        when(article.getTitle()).thenReturn("t");
        return article;
    }

    @Test
    void storesClaimsAndLinksAgreeingAndConflictingEvidenceBothWays() {

        Article article = articleInEvent(9L);
        when(articles.findById(1L)).thenReturn(Optional.of(article));
        when(client.extractClaims(any())).thenReturn(new ClaimExtractionResponse("cricket-1", List.of(
                extracted("runs_scored", "139", "title"))));
        when(claims.save(any())).thenAnswer(invocation -> {
            Claim saved = mock(Claim.class);
            Claim given = invocation.getArgument(0);
            when(saved.getId()).thenReturn(100L);
            when(saved.getArticleId()).thenReturn(given.getArticleId());
            when(saved.getEventId()).thenReturn(given.getEventId());
            when(saved.getSubjectNormalized()).thenReturn(given.getSubjectNormalized());
            when(saved.getPredicate()).thenReturn(given.getPredicate());
            when(saved.getUnit()).thenReturn(given.getUnit());
            when(saved.getValueText()).thenReturn(given.getValueText());
            return saved;
        });
        Claim agreeing = mock(Claim.class);
        when(agreeing.getId()).thenReturn(50L);
        when(agreeing.getArticleId()).thenReturn(2L);
        when(agreeing.getValueText()).thenReturn("139");
        Claim conflicting = mock(Claim.class);
        when(conflicting.getId()).thenReturn(51L);
        when(conflicting.getArticleId()).thenReturn(3L);
        when(conflicting.getValueText()).thenReturn("140");
        when(claims.findComparable(9L, "virat kohli", "runs_scored", "runs", 1L)).thenReturn(List.of(agreeing, conflicting));

        assertThat(service.recordClaims(1L)).isEqualTo(1);

        ArgumentCaptor<Claim> saved = ArgumentCaptor.forClass(Claim.class);
        verify(claims).save(saved.capture());
        assertThat(saved.getValue().getEventId()).isEqualTo(9L);
        verify(runs).runId("claim-extraction", "cricket-1", ExtractionRunService.NO_PROMPT);

        verify(claims).insertEvidenceIfAbsent(100L, 2L, "SUPPORTS");
        verify(claims).insertEvidenceIfAbsent(50L, 1L, "SUPPORTS");
        verify(claims).insertEvidenceIfAbsent(100L, 3L, "REFUTES");
        verify(claims).insertEvidenceIfAbsent(51L, 1L, "REFUTES");
    }

    @Test
    void skipsAlreadyRecordedArticlesAndSurvivesAiFailures() {

        Article recorded = articleInEvent(9L);
        Article fresh = articleInEvent(9L);

        when(articles.findById(1L)).thenReturn(Optional.of(recorded));
        when(claims.hasClaims(1L)).thenReturn(true);
        assertThat(service.recordClaims(1L)).isZero();

        when(articles.findById(2L)).thenReturn(Optional.of(fresh));
        when(client.extractClaims(any())).thenThrow(new ResourceAccessException("Read timed out"));
        assertThat(service.recordClaims(2L)).isZero();

        verify(claims, never()).save(any());
    }

    @Test
    void malformedClaimsAreDropped() {

        List<ClaimExtractionResponse.Claim> kept = ClaimService.valid(List.of(
                extracted("runs_scored", "139", "title"),
                extracted("vote_share", "40%", "title"),
                extracted("runs_scored", "139", "body"),
                new ClaimExtractionResponse.Claim("X", "x", "runs_scored", 1, "1", "runs", "X 1", 5, 2, "title")
        ));

        assertThat(kept).hasSize(1);
    }

    @Test
    void reviewsAreValidatedAndLoggedWithTheReviewer() {

        when(claims.existsById(7L)).thenReturn(true);
        when(reviews.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ClaimReview review = service.review(7L, " Approved ", "x".repeat(600), "admin@example.test").orElseThrow();

        assertThat(review.getDecision()).isEqualTo("approved");
        assertThat(review.getReviewer()).isEqualTo("admin@example.test");
        assertThat(review.getNote()).hasSize(500);

        assertThatThrownBy(() -> service.review(7L, "maybe", null, "a")).isInstanceOf(IllegalArgumentException.class);
        assertThat(service.review(404L, "rejected", null, "a")).isEmpty();
    }

    @Test
    void listingRejectsUnknownStatusesAndCapsTheLimit() {

        assertThatThrownBy(() -> service.list("all", null, 10)).isInstanceOf(IllegalArgumentException.class);

        service.list("pending", null, 1000);
        verify(claims).findWithStatus("pending", null, ClaimService.MAX_LIST);
        verify(claims, never()).findWithStatus(anyString(), anyLong(), anyInt());
    }

    @Test
    void sameValueSupportsOtherwiseRefutes() {

        assertThat(ClaimService.evidenceLabel("351/9", "351/9")).isEqualTo("SUPPORTS");
        assertThat(ClaimService.evidenceLabel("5 wickets", "5 Wickets")).isEqualTo("SUPPORTS");
        assertThat(ClaimService.evidenceLabel("351/9", "350/9")).isEqualTo("REFUTES");
    }
}
