package com.cfs.xnews.event;

import com.cfs.xnews.analysis.FactCheck;
import com.cfs.xnews.analysis.FactCheckRepository;
import com.cfs.xnews.event.dto.EventMatchConfidence;
import com.cfs.xnews.event.dto.EventSummaryProjection;
import com.cfs.xnews.event.dto.EventSummaryResponse;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsEventServiceTest {

    @Mock
    private NewsEventRepository eventRepository;

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private FactCheckRepository factCheckRepository;

    @Mock
    private EventMatchDecisionRepository eventMatchDecisionRepository;

    @InjectMocks
    private NewsEventService newsEventService;

    @Test
    void getMatchConfidence_unknownEvent_isEmpty() {

        when(eventRepository.existsById(404L)).thenReturn(false);

        assertThat(newsEventService.getMatchConfidence(404L)).isEmpty();
        verify(eventMatchDecisionRepository, never()).findAppliedForEvent(404L);
    }

    @Test
    void getMatchConfidence_reportsJoinedAndStartedDecisions() {

        when(eventRepository.existsById(5L)).thenReturn(true);
        when(eventMatchDecisionRepository.findAppliedForEvent(5L)).thenReturn(List.of(
                new EventMatchDecision(10L, "v1", "shadow", "v1-centroid", null, true, 0.41, 0.94, "[]", 20, null),
                new EventMatchDecision(11L, "v1", "shadow", "v1-centroid", 5L, true, 0.97, 0.94, "[]", 18, null)
        ));

        List<EventMatchConfidence> matches = newsEventService.getMatchConfidence(5L).orElseThrow();

        assertThat(matches).extracting(EventMatchConfidence::articleId).containsExactly(10L, 11L);
        assertThat(matches).extracting(EventMatchConfidence::decision).containsExactly("started", "joined");
        assertThat(matches.get(1).probability()).isEqualTo(0.97);
        assertThat(matches.get(1).threshold()).isEqualTo(0.94);
        assertThat(matches.get(1).matcher()).isEqualTo("v1");
    }

    @Test
    void deleteEvent_orphansArticlesAndRemovesFactChecksBeforeDeletingEvent() {

        NewsEvent event = new NewsEvent("title", "description");

        Article article = new Article(
                "title", "description", "http://example.com", "source", null
        );
        event.addArticle(article);
        event.getFactChecks().add(new FactCheck());

        when(eventRepository.findById(5L)).thenReturn(Optional.of(event));

        newsEventService.deleteEvent(5L);

        assertThat(article.getNewsEvent()).isNull();
        verify(articleRepository).saveAll(List.of(article));
        verify(factCheckRepository).deleteAll(event.getFactChecks());
        verify(eventRepository).delete(event);
    }

    @Test
    void getCoverage_unknownEvent_isEmpty() {

        when(eventRepository.findById(404L)).thenReturn(Optional.empty());

        assertThat(newsEventService.getCoverage(404L)).isEmpty();
    }

    @Test
    void getAllEvents_includesAnalysisFieldsInSummary() {

        EventSummaryProjection projection = mock(EventSummaryProjection.class);
        when(projection.getId()).thenReturn(7L);
        when(projection.getTitle()).thenReturn("title");
        when(projection.getGeneratedTitle()).thenReturn("Generated title");
        when(projection.getSourceCount()).thenReturn(3L);
        when(projection.getVerificationStatus()).thenReturn("CONTESTED");
        when(projection.getDisagreementLevel()).thenReturn("HIGH");

        when(eventRepository.findAllEventSummaries()).thenReturn(List.of(projection));

        List<EventSummaryResponse> events = newsEventService.getAllEvents();

        assertThat(events).hasSize(1);
        EventSummaryResponse event = events.get(0);
        assertThat(event.id()).isEqualTo(7L);
        // The first headline and the generated title are both returned.
        assertThat(event.title()).isEqualTo("title");
        assertThat(event.generatedTitle()).isEqualTo("Generated title");
        assertThat(event.sourceCount()).isEqualTo(3L);
        assertThat(event.verificationStatus()).isEqualTo("CONTESTED");
        assertThat(event.disagreementLevel()).isEqualTo("HIGH");
        // The deprecated misinformation risk is no longer part of the API.
        assertThat(EventSummaryResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .doesNotContain("misinformationRisk");
    }

    @Test
    void deleteEvent_throwsAndDoesNotTouchAnythingWhenMissing() {

        when(eventRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsEventService.deleteEvent(404L))
                .isInstanceOf(RuntimeException.class);

        verify(eventRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(factCheckRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
    }
}
