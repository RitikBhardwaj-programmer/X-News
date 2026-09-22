package com.cfs.xnews.event;

import com.cfs.xnews.analysis.FactCheck;
import com.cfs.xnews.analysis.FactCheckRepository;
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

    @InjectMocks
    private NewsEventService newsEventService;

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
    void deleteEvent_throwsAndDoesNotTouchAnythingWhenMissing() {

        when(eventRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsEventService.deleteEvent(404L))
                .isInstanceOf(RuntimeException.class);

        verify(eventRepository, never()).delete(org.mockito.ArgumentMatchers.any());
        verify(factCheckRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyList());
    }
}
