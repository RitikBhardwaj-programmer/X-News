package com.cfs.xnews.news.articles;

import com.cfs.xnews.kafka.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleServiceTest {

    @Mock
    private KafkaProducer kafkaProducer;

    @Mock
    private ArticleRepository articleRepository;

    @InjectMocks
    private ArticleService articleService;

    @Test
    void deleteArticle_deletesSingleRowWhenItExists() {

        when(articleRepository.existsById(1L)).thenReturn(true);

        articleService.deleteArticle(1L);

        verify(articleRepository).deleteById(1L);
    }

    @Test
    void deleteArticle_throwsAndDoesNotDeleteWhenMissing() {

        when(articleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> articleService.deleteArticle(99L))
                .isInstanceOf(RuntimeException.class);

        verify(articleRepository, never()).deleteById(any());
    }
}
