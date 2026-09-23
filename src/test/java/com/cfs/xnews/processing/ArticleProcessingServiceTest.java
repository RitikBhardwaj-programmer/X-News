package com.cfs.xnews.processing;

import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.news.articles.Article;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ArticleProcessingServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 20, 12, 0);

    @Test
    void temporalScoreBucketsTheGapToTheEventsLastActivity() {

        assertThat(ArticleProcessingService.calculateTemporalScore(NOW, NOW)).isEqualTo(1.0);
        assertThat(ArticleProcessingService.calculateTemporalScore(NOW, NOW.minusDays(1))).isEqualTo(0.9);
        assertThat(ArticleProcessingService.calculateTemporalScore(NOW, NOW.minusDays(3))).isEqualTo(0.8);
        assertThat(ArticleProcessingService.calculateTemporalScore(NOW, NOW.minusDays(7))).isEqualTo(0.6);
        assertThat(ArticleProcessingService.calculateTemporalScore(NOW, NOW.minusDays(30))).isEqualTo(0.4);
        assertThat(ArticleProcessingService.calculateTemporalScore(NOW, NOW.minusDays(31))).isEqualTo(0.1);
    }

    @Test
    void temporalScoreIsNeutralWhenADateIsMissing() {

        assertThat(ArticleProcessingService.calculateTemporalScore(null, NOW)).isEqualTo(0.5);
        assertThat(ArticleProcessingService.calculateTemporalScore(NOW, null)).isEqualTo(0.5);
    }

    @Test
    void probabilityBucketsMatchThePlannedDistribution() {

        assertThat(ArticleProcessingService.probabilityBucket(-1.0)).isEqualTo("none");
        assertThat(ArticleProcessingService.probabilityBucket(0.49)).isEqualTo("0.0-0.5");
        assertThat(ArticleProcessingService.probabilityBucket(0.5)).isEqualTo("0.5-0.7");
        assertThat(ArticleProcessingService.probabilityBucket(0.7)).isEqualTo("0.7-0.8");
        assertThat(ArticleProcessingService.probabilityBucket(0.85)).isEqualTo("0.8-0.9");
        assertThat(ArticleProcessingService.probabilityBucket(0.95)).isEqualTo("0.9-1.0");
    }

    @Test
    void addingArticlesMaintainsMemberCountAndActivityWindow() {

        NewsEvent event = new NewsEvent("title", "description");

        event.addArticle(article(NOW));
        event.addArticle(article(NOW.plusDays(2)));
        event.addArticle(article(NOW.minusDays(1)));

        assertThat(event.getMemberCount()).isEqualTo(3);
        assertThat(event.getFirstActivityAt()).isEqualTo(NOW.minusDays(1));
        assertThat(event.getLastActivityAt()).isEqualTo(NOW.plusDays(2));
    }

    private static Article article(LocalDateTime publishedAt) {

        return new Article("t", "d", "http://example.com/" + publishedAt, "s", publishedAt);
    }
}
