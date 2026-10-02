package com.cfs.xnews.ai;

import com.cfs.xnews.ai.EventAIAnalysis.CitedSentence;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CitedAnalysisTest {

    // The event's articles and their outlets.
    private static final Map<Long, String> EVENT = Map.of(
            1L, "The Hindu",
            2L, "The Hindu",
            3L, "NDTV"
    );

    private static CitedSentence fact(String text, Long... articles) {
        return new CitedSentence(null, text, List.of(articles));
    }

    private static CitedSentence framing(String outlet, String text, Long... articles) {
        return new CitedSentence(outlet, text, List.of(articles));
    }

    @Test
    void keepsCitedSentencesAndDropsUncitedOnes() {

        CitedAnalysis analysis = CitedAnalysis.validate(new EventAIAnalysis(
                List.of(fact("Rain closed schools.", 1L, 3L), fact("No source for this."), fact("Other event.", 99L)),
                List.of(),
                "low"
        ), EVENT);

        assertThat(analysis.agreedFacts()).extracting(CitedAnalysis.Fact::text).containsExactly("Rain closed schools.");
        assertThat(analysis.agreedFacts().get(0).articles()).containsExactly(1L, 3L);
        assertThat(analysis.droppedSentences()).isEqualTo(2);
        assertThat(analysis.disagreementLevel()).isEqualTo("LOW");
    }

    @Test
    void citationsOutsideTheEventAreRemoved() {

        CitedAnalysis analysis = CitedAnalysis.validate(new EventAIAnalysis(
                List.of(fact("Rain closed schools.", 1L, 42L, 1L)), null, "HIGH"), EVENT);

        assertThat(analysis.agreedFacts().get(0).articles()).containsExactly(1L);
    }

    @Test
    void theOutletComesFromTheCitedArticleAndAppearsOnce() {

        CitedAnalysis analysis = CitedAnalysis.validate(new EventAIAnalysis(
                List.of(fact("Rain closed schools.", 1L)),
                List.of(
                        framing("Some Other Paper", "Focuses on relief.", 3L),
                        framing("NDTV", "A second NDTV line.", 3L),
                        framing("The Hindu", "Uncited framing."),
                        framing("The Hindu", "Focuses on schools.", 2L)
                ),
                "MEDIUM"
        ), EVENT);

        assertThat(analysis.framing()).extracting(CitedAnalysis.Framing::outlet).containsExactly("NDTV", "The Hindu");
        assertThat(analysis.framing().get(0).text()).isEqualTo("Focuses on relief.");
        assertThat(analysis.droppedSentences()).isEqualTo(2);
        assertThat(analysis.framingText()).isEqualTo("NDTV: Focuses on relief.\nThe Hindu: Focuses on schools.");
    }

    @Test
    void unknownLevelsAndOverlongTextAreCleaned() {

        CitedAnalysis analysis = CitedAnalysis.validate(new EventAIAnalysis(
                List.of(fact("x".repeat(600), 1L)), null, "SEVERE"), EVENT);

        assertThat(analysis.disagreementLevel()).isNull();
        assertThat(analysis.agreedFacts().get(0).text()).hasSize(CitedAnalysis.MAX_TEXT_CHARS);
    }

    @Test
    void factsAreCappedAndSummaryIsTheirText() {

        List<CitedSentence> many = Collections.nCopies(8, fact("Same fact.", 1L));

        CitedAnalysis analysis = CitedAnalysis.validate(new EventAIAnalysis(many, null, "LOW"), EVENT);

        assertThat(analysis.agreedFacts()).hasSize(CitedAnalysis.MAX_FACTS);
        assertThat(analysis.droppedSentences()).isEqualTo(2);
        assertThat(analysis.summaryText()).startsWith("Same fact. Same fact.");
    }

    @Test
    void anOldFormatReplyHasNoFacts() {

        CitedAnalysis analysis = CitedAnalysis.validate(new EventAIAnalysis(null, null, "LOW"), EVENT);

        assertThat(analysis.agreedFacts()).isEmpty();
        assertThat(analysis.framing()).isEmpty();
    }
}
