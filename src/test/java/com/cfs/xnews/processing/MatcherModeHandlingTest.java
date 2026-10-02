package com.cfs.xnews.processing;

import com.cfs.xnews.event.CentroidUpdateStrategy;
import com.cfs.xnews.event.EventMatchDecision;
import com.cfs.xnews.event.EventMatchDecisionRepository;
import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventRepository;
import com.cfs.xnews.event.NewsEventService;
import com.cfs.xnews.event.dto.EventMatchResponse;
import com.cfs.xnews.event.dto.EventMatchResult;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.processing.processor.CategoryProcessor;
import com.cfs.xnews.processing.processor.ContentCleaner;
import com.cfs.xnews.processing.processor.KeywordProcessor;
import com.cfs.xnews.processing.processor.SentimentProcessor;
import com.cfs.xnews.provenance.ExtractionRunService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * ai.event-matcher.mode: which matcher decides and which decisions are stored.
 * Event 1 is the only candidate; v1 scores it 0.97 (attach at 0.94).
 */
class MatcherModeHandlingTest {

    private static final float[] EMBEDDING = {1f, 0f};

    private NewsEventService newsEventService;
    private NewsEventRepository newsEventRepository;
    private EventMatchingClient eventMatchingClient;
    private EventMatcherV2 eventMatcherV2;
    private EventMatchDecisionRepository decisions;
    private ExtractionRunService runs;

    private NewsEvent existing;
    private NewsEvent created;
    private Article article;

    @BeforeEach
    void setUp() {

        newsEventService = mock(NewsEventService.class);
        newsEventRepository = mock(NewsEventRepository.class);
        eventMatchingClient = mock(EventMatchingClient.class);
        eventMatcherV2 = mock(EventMatcherV2.class);
        decisions = mock(EventMatchDecisionRepository.class);
        runs = mock(ExtractionRunService.class);
        when(runs.runId(any(), any(), any())).thenReturn(42L);

        existing = mock(NewsEvent.class);
        when(existing.getId()).thenReturn(1L);
        when(existing.getCentroidEmbedding()).thenReturn(EMBEDDING);

        created = mock(NewsEvent.class);
        when(created.getId()).thenReturn(99L);

        article = new Article("t", "d", "http://example.com/a", "s", LocalDateTime.of(2026, 10, 2, 12, 0));

        when(newsEventRepository.findNearestOpenEvents(anyString(), anyInt())).thenReturn(List.of(existing));
        when(newsEventService.createEvent(any())).thenReturn(created);
        when(eventMatchingClient.predict(any())).thenReturn(
                new EventMatchResponse(List.of(new EventMatchResult(1L, 0.97, 0.9))));
    }

    private ArticleProcessingService service(String mode) {

        CentroidUpdateStrategy centroids = mock(CentroidUpdateStrategy.class);
        when(centroids.update(any(), anyInt(), any())).thenReturn(EMBEDDING);

        return new ArticleProcessingService(
                newsEventService,
                newsEventRepository,
                mock(SentimentProcessor.class),
                mock(CategoryProcessor.class),
                mock(KeywordProcessor.class),
                mock(ArticleRepository.class),
                mock(ContentCleaner.class),
                eventMatchingClient,
                centroids,
                eventMatcherV2,
                decisions,
                runs,
                0.94,
                30,
                mode
        );
    }

    private static EventMatcherV2.Outcome v2Chooses(Long eventId, double probability) {
        return new EventMatcherV2.Outcome(eventId, probability, 0.98, "v2-b/default", "[]", 12);
    }

    private List<EventMatchDecision> savedDecisions(int count) {

        ArgumentCaptor<EventMatchDecision> saved = ArgumentCaptor.forClass(EventMatchDecision.class);
        verify(decisions, times(count)).save(saved.capture());
        return saved.getAllValues();
    }

    @Test
    void v1ModeNeverCallsV2AndStoresNothing() {

        NewsEvent result = service("v1").matchOrCreateEvent(article, EMBEDDING, 0);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(eventMatcherV2, decisions);
    }

    @Test
    void shadowModeAppliesV1AndStoresBothDecisions() {

        when(eventMatcherV2.decide(any(), any(), anyString(), any())).thenReturn(v2Chooses(null, 0.6));

        NewsEvent result = service("shadow").matchOrCreateEvent(article, EMBEDDING, 0);

        assertThat(result).isSameAs(existing);
        verify(newsEventService, never()).createEvent(any());

        List<EventMatchDecision> saved = savedDecisions(2);

        assertThat(saved.get(0).getMatcher()).isEqualTo("v1");
        assertThat(saved.get(0).isApplied()).isTrue();
        assertThat(saved.get(0).getChosenEventId()).isEqualTo(1L);
        assertThat(saved.get(0).getMode()).isEqualTo("shadow");
        assertThat(saved.get(0).getCandidates()).contains("\"event_id\":1", "\"probability\":0.97");

        assertThat(saved.get(1).getMatcher()).isEqualTo("v2");
        assertThat(saved.get(1).isApplied()).isFalse();
        assertThat(saved.get(1).getChosenEventId()).isNull();
        assertThat(saved.get(1).getProbability()).isEqualTo(0.6);
        assertThat(saved.get(1).getThreshold()).isEqualTo(0.98);
        assertThat(saved.get(1).getModelVersion()).isEqualTo("v2-b/default");
        assertThat(saved).extracting(EventMatchDecision::getRunId).containsExactly(42L, 42L);
        verify(runs).runId("event-matcher", "v1-centroid@0.94", ExtractionRunService.NO_PROMPT);
        verify(runs).runId("event-matcher", "v2-b/default", ExtractionRunService.NO_PROMPT);
    }

    @Test
    void v2ModeLetsV2DecideWithoutCallingV1() {

        when(eventMatcherV2.decide(any(), any(), anyString(), any())).thenReturn(v2Chooses(null, 0.6));

        // v1 would attach to event 1 (0.97 >= 0.94); v2 creates a new event.
        NewsEvent result = service("v2").matchOrCreateEvent(article, EMBEDDING, 0);

        assertThat(result).isSameAs(created);
        verify(eventMatchingClient, never()).predict(any());

        EventMatchDecision saved = savedDecisions(1).get(0);

        assertThat(saved.getMatcher()).isEqualTo("v2");
        assertThat(saved.isApplied()).isTrue();
        assertThat(saved.getMode()).isEqualTo("live");
    }

    @Test
    void v2ModeAttachesToTheEventV2Chose() {

        when(eventMatcherV2.decide(any(), any(), anyString(), any())).thenReturn(v2Chooses(1L, 0.99));

        assertThat(service("v2").matchOrCreateEvent(article, EMBEDDING, 0)).isSameAs(existing);
        verify(existing).addArticle(article);
    }

    @Test
    void v2FailureFallsBackToV1AndRecordsTheError() {

        when(eventMatcherV2.decide(any(), any(), anyString(), any()))
                .thenThrow(new ResourceAccessException("Read timed out"));

        NewsEvent result = service("v2").matchOrCreateEvent(article, EMBEDDING, 0);

        assertThat(result).isSameAs(existing);

        List<EventMatchDecision> saved = savedDecisions(2);

        assertThat(saved.get(0).getMatcher()).isEqualTo("v1");
        assertThat(saved.get(0).isApplied()).isTrue();
        assertThat(saved.get(1).getMatcher()).isEqualTo("v2");
        assertThat(saved.get(1).isApplied()).isFalse();
        assertThat(saved.get(1).getThreshold()).isNull();
        assertThat(saved.get(1).getError()).contains("ResourceAccessException", "Read timed out");
    }
}
