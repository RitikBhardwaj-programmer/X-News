package com.cfs.xnews.processing;

import com.cfs.xnews.event.EventMatchingClient;
import com.cfs.xnews.event.dto.VocabularyRefitRequest;
import com.cfs.xnews.event.dto.VocabularyRefitResponse;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class VocabularyRefitSchedulerTest {

    private ArticleRepository articleRepository;
    private EventMatchingClient client;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        articleRepository = mock(ArticleRepository.class);
        client = mock(EventMatchingClient.class);
        clock = new MutableClock(Instant.parse("2026-10-03T02:00:00Z"));
    }

    private VocabularyRefitScheduler scheduler(MatcherMode mode) {
        return new VocabularyRefitScheduler(articleRepository, client, mode, clock);
    }

    private void articles(int count) {

        List<Object[]> rows = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            rows.add(new Object[]{"Title " + i, i % 2 == 0 ? "Description " + i : null});
        }
        rows.add(new Object[]{"  ", "blank titles are skipped"});

        when(articleRepository.findRecentTexts(any(), anyInt())).thenReturn(rows);
    }

    @Test
    void refitSendsTheLastFourteenDaysOfArticles() {

        articles(150);
        when(client.refitVocabulary(any())).thenReturn(new VocabularyRefitResponse("2026-10-03T02:00:01Z/150", 150));

        assertThat(scheduler(MatcherMode.SHADOW).refit()).isTrue();

        verify(articleRepository).findRecentTexts(
                eq(LocalDateTime.of(2026, 9, 19, 2, 0)),
                eq(VocabularyRefitScheduler.MAX_ITEMS)
        );

        ArgumentCaptor<VocabularyRefitRequest> request = ArgumentCaptor.forClass(VocabularyRefitRequest.class);
        verify(client).refitVocabulary(request.capture());

        assertThat(request.getValue().items()).hasSize(150);
        assertThat(request.getValue().items().get(1).description()).isNull();
    }

    @Test
    void tooFewArticlesSkipTheRefit() {

        articles(99);

        assertThat(scheduler(MatcherMode.SHADOW).refit()).isFalse();
        verify(client, never()).refitVocabulary(any());
    }

    @Test
    void aFailedRefitIsLoggedNotThrown() {

        articles(150);
        when(client.refitVocabulary(any())).thenThrow(new ResourceAccessException("Connection refused"));

        assertThat(scheduler(MatcherMode.SHADOW).refit()).isFalse();
    }

    @Test
    void aRequestedRefitRunsOnceAndRetriesAtMostEveryTenMinutes() {

        articles(150);
        when(client.refitVocabulary(any())).thenThrow(new ResourceAccessException("Connection refused"));

        VocabularyRefitScheduler scheduler = scheduler(MatcherMode.SHADOW);

        scheduler.refitIfRequested();
        verify(client, never()).refitVocabulary(any());

        scheduler.requestRefit();
        scheduler.refitIfRequested();
        scheduler.refitIfRequested();
        verify(client, times(1)).refitVocabulary(any());

        scheduler.requestRefit();
        clock.advance(Duration.ofMinutes(9));
        scheduler.refitIfRequested();
        verify(client, times(1)).refitVocabulary(any());

        clock.advance(Duration.ofMinutes(1));
        scheduler.refitIfRequested();
        verify(client, times(2)).refitVocabulary(any());
    }

    @Test
    void v1ModeNeverRefits() {

        VocabularyRefitScheduler scheduler = scheduler(MatcherMode.V1);

        scheduler.requestRefit();
        scheduler.refitIfRequested();
        scheduler.nightlyRefit();

        verifyNoInteractions(articleRepository, client);
    }

    private static final class MutableClock extends Clock {

        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
