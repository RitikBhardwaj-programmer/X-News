package com.cfs.xnews.storyline;

import com.cfs.xnews.event.NewsEventRepository;
import com.cfs.xnews.provenance.ExtractionRunService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StorylineTest {

    private final EventRelationRepository relations = mock(EventRelationRepository.class);
    private final NewsEventRepository events = mock(NewsEventRepository.class);

    @Test
    void nightlyJobLinksTheLastFourteenDaysWithItsRun() {

        ExtractionRunService runs = mock(ExtractionRunService.class);
        when(runs.runId("event-relations", EventRelationScheduler.RUN_MODEL, ExtractionRunService.NO_PROMPT))
                .thenReturn(12L);
        when(relations.linkRecentEvents(LocalDateTime.of(2026, 9, 20, 4, 0), 2, 0.6, 5, 12L)).thenReturn(7);

        Clock clock = Clock.fixed(Instant.parse("2026-10-04T04:00:00Z"), ZoneOffset.UTC);

        assertThat(new EventRelationScheduler(relations, runs, clock).linkNightly()).isEqualTo(7);
        assertThat(EventRelationScheduler.RUN_MODEL).isEqualTo("entities>=2,cosine>=0.6,window=14d");
    }

    @Test
    void timelineMarksTheCurrentEventAndKeepsOrder() {

        when(events.existsById(5L)).thenReturn(true);
        when(relations.findStoryline(5L, 2, 30)).thenReturn(List.<Object[]>of(
                new Object[]{4L, "1st ODI", Timestamp.valueOf("2026-09-28 10:00:00"), Timestamp.valueOf("2026-09-28 20:00:00"), 6, 1},
                new Object[]{5L, "2nd ODI", Timestamp.valueOf("2026-09-30 10:00:00"), Timestamp.valueOf("2026-09-30 22:00:00"), 9, 0}
        ));

        var response = new StorylineController(relations, events).getTimeline(5L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).extracting(StorylineController.TimelineEntry::eventId).containsExactly(4L, 5L);
        assertThat(response.getBody()).extracting(StorylineController.TimelineEntry::current).containsExactly(false, true);
        assertThat(response.getBody().get(0).firstActivity()).isEqualTo(LocalDateTime.of(2026, 9, 28, 10, 0));
        assertThat(response.getBody().get(1).articleCount()).isEqualTo(9);
    }

    @Test
    void unknownEventIsNotFound() {

        when(events.existsById(404L)).thenReturn(false);

        assertThat(new StorylineController(relations, events).getTimeline(404L).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        verify(relations, never()).findStoryline(anyLong(), anyInt(), anyInt());
    }
}
