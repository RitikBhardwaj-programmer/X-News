package com.cfs.xnews.title;

import com.cfs.xnews.ai.AIService;
import com.cfs.xnews.ai.AIServiceUnavailableException;
import com.cfs.xnews.ai.EventAITitle;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventRepository;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.provenance.ExtractionRunService;
import com.google.genai.errors.ClientException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EventTitleServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
    private static final LocalDateTime NOW_UTC = LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);

    private NewsEventRepository eventRepository;
    private AIService aiService;
    private ExtractionRunService extractionRunService;
    private EventTitleService service;

    @BeforeEach
    void setUp() {
        eventRepository = mock(NewsEventRepository.class);
        aiService = mock(AIService.class);
        extractionRunService = mock(ExtractionRunService.class);
        service = new EventTitleService(eventRepository, aiService, extractionRunService, Clock.fixed(NOW, ZoneOffset.UTC));

        when(aiService.modelName()).thenReturn("gemini-test");
        when(extractionRunService.runId(EventTitleService.RUN_KIND, "gemini-test", AIService.TITLE_PROMPT_VERSION))
                .thenReturn(42L);
    }

    private NewsEvent eventWithArticles(int count) {

        NewsEvent event = new NewsEvent("First headline", "description");
        ReflectionTestUtils.setField(event, "id", 7L);

        for (long id = 1; id <= count; id++) {
            Article article = new Article("t" + id, "d", "http://example.com/" + id, "The Hindu -> India", LocalDateTime.now());
            ReflectionTestUtils.setField(article, "id", id);
            event.addArticle(article);
        }

        when(eventRepository.findById(7L)).thenReturn(Optional.of(event));
        return event;
    }

    @Test
    void storesAValidTitleWithItsRunAndKeepsTheFirstHeadline() {

        NewsEvent event = eventWithArticles(6);
        when(aiService.titleEvent(event)).thenReturn(new EventAITitle("Rain closes schools", List.of(1L, 2L)));

        assertThat(service.title(7L)).isEqualTo(EventTitleService.Outcome.TITLED);

        assertThat(event.getGeneratedTitle()).isEqualTo("Rain closes schools");
        assertThat(event.getTitleRunId()).isEqualTo(42L);
        assertThat(event.getTitleAttemptedAt()).isEqualTo(NOW_UTC);
        assertThat(event.getTitle()).isEqualTo("First headline");
    }

    @Test
    void discardsATitleThatFailsValidationButRecordsTheAttempt() {

        NewsEvent event = eventWithArticles(5);
        when(aiService.titleEvent(event)).thenReturn(new EventAITitle("Rain closes schools", List.of(1L)));

        assertThat(service.title(7L)).isEqualTo(EventTitleService.Outcome.DISCARDED);

        assertThat(event.getGeneratedTitle()).isNull();
        assertThat(event.getTitleRunId()).isNull();
        assertThat(event.getTitleAttemptedAt()).isEqualTo(NOW_UTC);
    }

    @Test
    void anUnusableReplyRecordsTheAttempt() {

        NewsEvent event = eventWithArticles(5);
        when(aiService.titleEvent(event)).thenThrow(new RuntimeException("Failed to parse Gemini title response: x"));

        assertThat(service.title(7L)).isEqualTo(EventTitleService.Outcome.DISCARDED);
        assertThat(event.getTitleAttemptedAt()).isEqualTo(NOW_UTC);
    }

    @Test
    void aRequestGeminiRejectsForThisEventRecordsTheAttempt() {

        NewsEvent event = eventWithArticles(5);
        when(aiService.titleEvent(event)).thenThrow(new ClientException(400, "INVALID_ARGUMENT", "bad request"));

        assertThat(service.title(7L)).isEqualTo(EventTitleService.Outcome.DISCARDED);
        assertThat(event.getTitleAttemptedAt()).isEqualTo(NOW_UTC);
    }

    @Test
    void keyAndRateLimitErrorsAreRethrownSoTheEventIsTriedAgain() {

        NewsEvent event = eventWithArticles(5);

        when(aiService.titleEvent(event)).thenThrow(new ClientException(403, "PERMISSION_DENIED", "bad key"));
        assertThatThrownBy(() -> service.title(7L)).isInstanceOf(ClientException.class);

        doThrow(new AIServiceUnavailableException("busy", new ClientException(429, "RESOURCE_EXHAUSTED", "quota")))
                .when(aiService).titleEvent(event);
        assertThatThrownBy(() -> service.title(7L)).isInstanceOf(AIServiceUnavailableException.class);

        assertThat(event.getGeneratedTitle()).isNull();
    }

    @Test
    void skipsEventsWithFewerThanFiveArticlesWithoutCallingGemini() {

        NewsEvent event = eventWithArticles(4);

        assertThat(service.title(7L)).isEqualTo(EventTitleService.Outcome.SKIPPED);
        verify(aiService, never()).titleEvent(any());
        assertThat(event.getTitleAttemptedAt()).isNull();
    }

    @Test
    void skipsAMissingEvent() {

        when(eventRepository.findById(8L)).thenReturn(Optional.empty());

        assertThat(service.title(8L)).isEqualTo(EventTitleService.Outcome.SKIPPED);
        verify(aiService, never()).titleEvent(any());
    }
}
