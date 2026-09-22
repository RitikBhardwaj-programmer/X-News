package com.cfs.xnews.processing.processor;

import com.cfs.xnews.jev.JevClient;
import com.cfs.xnews.jev.dto.JevAnswer;
import com.cfs.xnews.jev.dto.JevResponse;
import com.cfs.xnews.news.articles.Article;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryProcessorTest {

    @Mock
    private JevClient jevClient;

    @Test
    void classify_usesJevChoiceWhenCallSucceeds() {

        CategoryProcessor categoryProcessor = new CategoryProcessor(jevClient);

        Article article = new Article(
                "Some headline about markets",
                "description",
                "http://example.com/a",
                "example",
                null
        );

        JevAnswer answer = new JevAnswer(
                "choice",
                null,
                "BUSINESS",
                Map.of("BUSINESS", 0.9),
                0.9,
                null,
                null
        );

        when(jevClient.evaluate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new JevResponse("jev-1.0", Map.of("category", answer), null));

        assertThat(categoryProcessor.classify(article)).isEqualTo("BUSINESS");
    }

    @Test
    void classify_fallsBackToKeywordsWhenJevCallFails() {

        CategoryProcessor categoryProcessor = new CategoryProcessor(jevClient);

        Article article = new Article(
                "OpenAI releases new model",
                "A story about artificial intelligence",
                "http://example.com/b",
                "example",
                null
        );

        when(jevClient.evaluate(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("service unavailable"));

        assertThat(categoryProcessor.classify(article)).isEqualTo("AI");
    }

    @Test
    void classify_fallsBackToKeywordsWhenJevReturnsNoChoice() {

        CategoryProcessor categoryProcessor = new CategoryProcessor(jevClient);

        Article article = new Article(
                "Local team wins championship",
                "A story about football",
                "http://example.com/c",
                "example",
                null
        );

        when(jevClient.evaluate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new JevResponse("jev-1.0", Map.of(), null));

        assertThat(categoryProcessor.classify(article)).isEqualTo("SPORTS");
    }
}
