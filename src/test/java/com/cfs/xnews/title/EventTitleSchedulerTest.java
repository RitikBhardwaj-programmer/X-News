package com.cfs.xnews.title;

import com.cfs.xnews.ai.AIServiceUnavailableException;
import com.cfs.xnews.event.NewsEventRepository;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static com.cfs.xnews.title.EventTitleService.Outcome.DISCARDED;
import static com.cfs.xnews.title.EventTitleService.Outcome.TITLED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EventTitleSchedulerTest {

    private static final int MIN = EventTitleService.MIN_ARTICLES;

    private NewsEventRepository eventRepository;
    private EventTitleService titleService;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        eventRepository = mock(NewsEventRepository.class);
        titleService = mock(EventTitleService.class);
        clock = new MutableClock(Instant.parse("2026-10-08T10:00:00Z"));
    }

    private EventTitleScheduler scheduler(boolean enabled, int maxPerRun, int maxPerDay) {
        return new EventTitleScheduler(eventRepository, titleService, enabled, maxPerRun, maxPerDay, clock);
    }

    private static AIServiceUnavailableException busy(int code) {
        return new AIServiceUnavailableException("busy", code == 429
                ? new ClientException(429, "RESOURCE_EXHAUSTED", "quota")
                : new ServerException(code, "UNAVAILABLE", "overloaded"));
    }

    @Test
    void doesNothingWhenDisabled() {

        assertThat(scheduler(false, 5, 12).titleEvents()).isZero();
        verifyNoInteractions(eventRepository, titleService);
    }

    @Test
    void titlesTheCandidatesOfThisRun() {

        when(eventRepository.findEventIdsToTitle(MIN, 5)).thenReturn(List.of(1L, 2L, 3L));
        when(titleService.title(1L)).thenReturn(TITLED);
        when(titleService.title(2L)).thenReturn(DISCARDED);
        when(titleService.title(3L)).thenReturn(TITLED);

        assertThat(scheduler(true, 5, 12).titleEvents()).isEqualTo(2);
    }

    @Test
    void theDailyBudgetComesFromTodaysAttemptsInTheDatabase() {

        when(eventRepository.countTitleAttemptsSince(LocalDateTime.of(2026, 10, 8, 0, 0))).thenReturn(10L);
        when(eventRepository.findEventIdsToTitle(MIN, 2)).thenReturn(List.of(1L, 2L));
        when(titleService.title(anyLong())).thenReturn(TITLED);

        assertThat(scheduler(true, 5, 12).titleEvents()).isEqualTo(2);
    }

    @Test
    void noCallsOnceTodaysBudgetIsUsed() {

        when(eventRepository.countTitleAttemptsSince(any())).thenReturn(12L);

        assertThat(scheduler(true, 5, 12).titleEvents()).isZero();
        verify(eventRepository, never()).findEventIdsToTitle(anyInt(), anyInt());
        verifyNoInteractions(titleService);
    }

    @Test
    void aRateLimitPausesTitlingUntilTheNextUtcDay() {

        EventTitleScheduler scheduler = scheduler(true, 5, 12);
        when(eventRepository.findEventIdsToTitle(anyInt(), anyInt())).thenReturn(List.of(1L, 2L));
        when(titleService.title(1L)).thenThrow(busy(429));

        assertThat(scheduler.titleEvents()).isZero();
        verify(titleService, never()).title(2L);

        // Same day: no further calls at all.
        assertThat(scheduler.titleEvents()).isZero();
        verify(eventRepository, times(1)).findEventIdsToTitle(anyInt(), anyInt());
        verify(eventRepository, times(1)).countTitleAttemptsSince(any());

        // Next UTC day: titling resumes.
        doReturn(TITLED).when(titleService).title(anyLong());
        clock.instant = Instant.parse("2026-10-09T00:05:00Z");
        assertThat(scheduler.titleEvents()).isEqualTo(2);
    }

    @Test
    void aBusyModelStopsOnlyThisRun() {

        EventTitleScheduler scheduler = scheduler(true, 5, 12);
        when(eventRepository.findEventIdsToTitle(anyInt(), anyInt())).thenReturn(List.of(1L, 2L));
        when(titleService.title(1L)).thenThrow(busy(503)).thenReturn(TITLED);
        when(titleService.title(2L)).thenReturn(TITLED);

        assertThat(scheduler.titleEvents()).isZero();
        verify(titleService, never()).title(2L);

        assertThat(scheduler.titleEvents()).isEqualTo(2);
    }

    @Test
    void anotherFailureSkipsOnlyThatEvent() {

        when(eventRepository.findEventIdsToTitle(MIN, 5)).thenReturn(List.of(1L, 2L));
        when(titleService.title(1L)).thenThrow(new IllegalStateException("database hiccup"));
        when(titleService.title(2L)).thenReturn(TITLED);

        assertThat(scheduler(true, 5, 12).titleEvents()).isEqualTo(1);
    }

    private static final class MutableClock extends Clock {

        Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
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
            return instant;
        }
    }
}
