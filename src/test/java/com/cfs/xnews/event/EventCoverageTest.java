package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EventCoverageResponse;
import com.cfs.xnews.event.dto.EventCoverageResponse.OutletCoverage;
import com.cfs.xnews.event.dto.EventCoverageResponse.TimelineEntry;
import com.cfs.xnews.news.articles.Article;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EventCoverageTest {

    private static final LocalDateTime NOON = LocalDateTime.of(2026, 9, 29, 12, 0);

    private static Article article(String title, String source, LocalDateTime publishedAt) {
        return new Article(title, "description", "http://example.com/" + title, source, publishedAt);
    }

    @Test
    void groupsArticlesByOutletAndListsTheirFeeds() {

        EventCoverageResponse coverage = NewsEventService.buildCoverage(1L, List.of(
                article("a", "The Hindu -> India", NOON),
                article("b", "Sportstar -> Cricket", NOON.plusHours(1)),
                article("c", "The Hindu -> cricket", NOON.plusHours(2)),
                article("d", "The Hindu -> India", NOON.plusHours(3))
        ));

        assertThat(coverage.articleCount()).isEqualTo(4);
        assertThat(coverage.outletCount()).isEqualTo(2);
        assertThat(coverage.outlets()).extracting(OutletCoverage::outlet)
                .containsExactly("The Hindu", "Sportstar");

        OutletCoverage hindu = coverage.outlets().get(0);
        assertThat(hindu.articleCount()).isEqualTo(3);
        assertThat(hindu.feeds()).containsExactly("India", "cricket");
        assertThat(hindu.firstSeen()).isEqualTo(NOON);
    }

    @Test
    void timelineIsInPublishOrderWithFirstAndLastSeen() {

        EventCoverageResponse coverage = NewsEventService.buildCoverage(1L, List.of(
                article("late", "A -> x", NOON.plusDays(1)),
                article("early", "B -> x", NOON.minusHours(5)),
                article("middle", "C -> x", NOON)
        ));

        assertThat(coverage.timeline()).extracting(TimelineEntry::title)
                .containsExactly("early", "middle", "late");
        assertThat(coverage.firstSeen()).isEqualTo(NOON.minusHours(5));
        assertThat(coverage.lastSeen()).isEqualTo(NOON.plusDays(1));
    }

    @Test
    void missingPublishTimeFallsBackToWhenItWasCollected() {

        Article undated = article("undated", "A -> x", null);

        EventCoverageResponse coverage = NewsEventService.buildCoverage(1L, List.of(
                article("old", "B -> x", NOON.minusYears(1)),
                undated
        ));

        TimelineEntry entry = coverage.timeline().get(1);
        assertThat(entry.title()).isEqualTo("undated");
        assertThat(entry.publishedAt()).isNull();
        assertThat(entry.observedAt()).isEqualTo(undated.getCreatedAt());
        assertThat(coverage.lastSeen()).isEqualTo(undated.getCreatedAt());
    }

    @Test
    void sourceWithoutSectionIsItsOwnOutlet() {

        assertThat(NewsEventService.outletOf("Reuters")).isEqualTo("Reuters");
        assertThat(NewsEventService.feedOf("Reuters")).isEmpty();
        assertThat(NewsEventService.outletOf("Ndtv news -> Cricket")).isEqualTo("Ndtv news");
        assertThat(NewsEventService.feedOf("Ndtv news -> Cricket")).isEqualTo("Cricket");
        assertThat(NewsEventService.outletOf(null)).isEqualTo("Unknown");

        EventCoverageResponse coverage = NewsEventService.buildCoverage(1L, List.of(article("a", "Reuters", NOON)));
        assertThat(coverage.outlets().get(0).feeds()).isEmpty();
    }

    @Test
    void emptyEventHasNoCoverage() {

        EventCoverageResponse coverage = NewsEventService.buildCoverage(9L, List.of());

        assertThat(coverage.eventId()).isEqualTo(9L);
        assertThat(coverage.articleCount()).isZero();
        assertThat(coverage.outletCount()).isZero();
        assertThat(coverage.firstSeen()).isNull();
        assertThat(coverage.lastSeen()).isNull();
        assertThat(coverage.timeline()).isEmpty();
    }
}
