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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

        when(jevClient.isConfigured()).thenReturn(true);
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

        when(jevClient.isConfigured()).thenReturn(true);
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

        when(jevClient.isConfigured()).thenReturn(true);
        when(jevClient.evaluate(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new JevResponse("jev-1.0", Map.of(), null));

        assertThat(categoryProcessor.classify(article)).isEqualTo("SPORTS");
    }

    @Test
    void classify_skipsJevAndUsesKeywordsWhenNoApiKeyConfigured() {

        CategoryProcessor categoryProcessor = new CategoryProcessor(jevClient);

        Article article = new Article(
                "OpenAI releases new model",
                "A story about artificial intelligence",
                "http://example.com/d",
                "example",
                null
        );

        when(jevClient.isConfigured()).thenReturn(false);

        assertThat(categoryProcessor.classify(article)).isEqualTo("AI");
        verify(jevClient, never()).evaluate(org.mockito.ArgumentMatchers.any());
    }

    private String keywordCategory(String title, String description) {

        when(jevClient.isConfigured()).thenReturn(false);

        return new CategoryProcessor(jevClient).classify(
                new Article(title, description, "http://example.com/" + title.hashCode(), "example", null));
    }

    @Test
    void keywordsMatchWholeWordsNotPartsOfWords() {

        // "ai" inside "said", "rain", "Chennai"; "sport" inside "transport";
        // "market" inside "supermarket".
        assertThat(keywordCategory("Heavy rain lashes Chennai, schools shut", "Officials said more showers are likely"))
                .isEqualTo("OTHER");
        assertThat(keywordCategory("Transport minister inaugurates new bus depot", null))
                .isEqualTo("POLITICS");
        assertThat(keywordCategory("Supermarket chain opens 20 stores", null))
                .isEqualTo("OTHER");
    }

    @Test
    void keywordsStillMatchWholeWordsAndPlurals() {

        assertThat(keywordCategory("New AI model beats benchmarks", null)).isEqualTo("AI");
        assertThat(keywordCategory("Stock markets fall on rate fears", null)).isEqualTo("BUSINESS");
        assertThat(keywordCategory("Assembly elections: polling ends", null)).isEqualTo("POLITICS");
        assertThat(keywordCategory("IND vs WI: cricket fans throng stadium", null)).isEqualTo("SPORTS");
    }
}
