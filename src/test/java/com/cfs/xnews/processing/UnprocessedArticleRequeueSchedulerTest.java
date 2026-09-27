package com.cfs.xnews.processing;

import com.cfs.xnews.kafka.ArticleEvent;
import com.cfs.xnews.kafka.KafkaProducer;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnprocessedArticleRequeueSchedulerTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private KafkaProducer kafkaProducer;

    @Test
    void requeue_publishesEveryStuckArticleInTheWindow() {

        Article article = new Article("title", "description", "http://example.com/a", "source", null);

        ArgumentCaptor<LocalDateTime> from = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> to = ArgumentCaptor.forClass(LocalDateTime.class);
        when(articleRepository.findByProcessedFalseAndCreatedAtBetween(from.capture(), to.capture()))
                .thenReturn(List.of(article));

        new UnprocessedArticleRequeueScheduler(articleRepository, kafkaProducer).requeueUnprocessedArticles();

        ArgumentCaptor<ArticleEvent> sent = ArgumentCaptor.forClass(ArticleEvent.class);
        verify(kafkaProducer).publishArticle(sent.capture());
        assertThat(sent.getValue().url()).isEqualTo("http://example.com/a");

        // Window: older than 10 minutes (in-flight work untouched), newer than 24 hours.
        assertThat(Duration.between(from.getValue(), to.getValue()))
                .isEqualTo(Duration.ofHours(24).minusMinutes(10));
    }

    @Test
    void requeue_doesNothingWhenNothingIsStuck() {

        when(articleRepository.findByProcessedFalseAndCreatedAtBetween(any(), any()))
                .thenReturn(List.of());

        new UnprocessedArticleRequeueScheduler(articleRepository, kafkaProducer).requeueUnprocessedArticles();

        verify(kafkaProducer, never()).publishArticle(any());
    }
}
