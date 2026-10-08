package com.cfs.xnews.title;

import com.cfs.xnews.ai.EventAITitle;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeneratedTitleTest {

    private static final Set<Long> EVENT_ARTICLES = Set.of(1L, 2L, 3L);

    private static Optional<GeneratedTitle> validate(String title, Long... articles) {
        return GeneratedTitle.validate(new EventAITitle(title, Arrays.asList(articles)), EVENT_ARTICLES);
    }

    @Test
    void keepsAShortTitleThatCitesTwoOfTheEventsArticles() {

        Optional<GeneratedTitle> title = validate("India win Asian Games cricket gold", 1L, 2L);

        assertThat(title).isPresent();
        assertThat(title.get().text()).isEqualTo("India win Asian Games cricket gold");
        assertThat(title.get().articles()).containsExactly(1L, 2L);
    }

    @Test
    void dropsATitleCitingFewerThanTwoArticles() {

        assertThat(validate("India win gold", 1L)).isEmpty();
        assertThat(validate("India win gold")).isEmpty();
    }

    @Test
    void citationsToOtherArticlesDoNotCount() {

        assertThat(validate("India win gold", 1L, 99L)).isEmpty();
        assertThat(validate("India win gold", 1L, 99L, 3L).orElseThrow().articles()).containsExactly(1L, 3L);
    }

    @Test
    void aRepeatedCitationCountsOnce() {

        assertThat(validate("India win gold", 2L, 2L)).isEmpty();
    }

    @Test
    void nullCitationsAreIgnored() {

        assertThat(validate("India win gold", null, 1L, 2L).orElseThrow().articles()).containsExactly(1L, 2L);
    }

    @Test
    void dropsABlankTitleOrTheModelsNoAgreementAnswer() {

        assertThat(validate("   ", 1L, 2L)).isEmpty();
        assertThat(validate("", 1L, 2L)).isEmpty();
        assertThat(GeneratedTitle.validate(new EventAITitle("", List.of()), EVENT_ARTICLES)).isEmpty();
    }

    @Test
    void dropsMissingFields() {

        assertThat(GeneratedTitle.validate(null, EVENT_ARTICLES)).isEmpty();
        assertThat(GeneratedTitle.validate(new EventAITitle(null, List.of(1L, 2L)), EVENT_ARTICLES)).isEmpty();
        assertThat(GeneratedTitle.validate(new EventAITitle("Title", null), EVENT_ARTICLES)).isEmpty();
    }

    @Test
    void keepsUpTo120CharactersAndDropsLonger() {

        assertThat(validate("a".repeat(120), 1L, 2L)).isPresent();
        assertThat(validate("a".repeat(121), 1L, 2L)).isEmpty();
    }

    @Test
    void cleansQuotesAndWhitespaceButKeepsAbbreviations() {

        assertThat(validate("  \"India  win\n gold\"  ", 1L, 2L).orElseThrow().text()).isEqualTo("India win gold");
        assertThat(validate("India signs trade deal with the U.S.", 1L, 2L).orElseThrow().text())
                .isEqualTo("India signs trade deal with the U.S.");
        assertThat(validate("“India win gold”", 1L, 2L).orElseThrow().text()).isEqualTo("India win gold");
        assertThat(validate("\"\"", 1L, 2L)).isEmpty();
    }
}
