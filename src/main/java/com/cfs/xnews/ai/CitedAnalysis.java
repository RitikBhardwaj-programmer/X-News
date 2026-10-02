package com.cfs.xnews.ai;

import com.cfs.xnews.ai.EventAIAnalysis.CitedSentence;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * "Cite or drop" (V4 roadmap step 4, information-layer spec section 7): a
 * sentence from the model is kept only if it cites at least one article of
 * the event. Citations to other articles are removed; an outlet is taken
 * from the cited article, never from the model.
 */
public record CitedAnalysis(
        List<Fact> agreedFacts,
        List<Framing> framing,
        String disagreementLevel,
        int droppedSentences
) {

    public record Fact(String text, List<Long> articles) {
    }

    public record Framing(String outlet, String text, List<Long> articles) {
    }

    static final int MAX_TEXT_CHARS = 500;
    static final int MAX_FACTS = 6;
    static final int MAX_FRAMINGS = 8;
    static final Set<String> LEVELS = Set.of("LOW", "MEDIUM", "HIGH");

    /** outletByArticle: the event's article ids and each one's outlet. */
    public static CitedAnalysis validate(EventAIAnalysis raw, Map<Long, String> outletByArticle) {

        int dropped = 0;

        List<Fact> facts = new ArrayList<>();
        for (CitedSentence sentence : orEmpty(raw.agreedFacts())) {
            List<Long> cited = validCitations(sentence, outletByArticle);
            if (cited.isEmpty() || facts.size() >= MAX_FACTS) {
                dropped++;
                continue;
            }
            facts.add(new Fact(clip(sentence.text()), cited));
        }

        List<Framing> framing = new ArrayList<>();
        Set<String> outlets = new LinkedHashSet<>();
        for (CitedSentence sentence : orEmpty(raw.framing())) {
            List<Long> cited = validCitations(sentence, outletByArticle);
            String outlet = cited.isEmpty() ? null : outletByArticle.get(cited.get(0));
            // One framing per outlet; the first one wins.
            if (outlet == null || !outlets.add(outlet) || framing.size() >= MAX_FRAMINGS) {
                dropped++;
                continue;
            }
            framing.add(new Framing(outlet, clip(sentence.text()), cited));
        }

        String level = raw.disagreementLevel() == null ? null : raw.disagreementLevel().trim().toUpperCase();

        return new CitedAnalysis(facts, framing, LEVELS.contains(level) ? level : null, dropped);
    }

    // Derived plain text for older clients (summary and biasAnalysis).
    public String summaryText() {
        return agreedFacts.stream().map(Fact::text).collect(Collectors.joining(" "));
    }

    public String framingText() {
        return framing.stream().map(f -> f.outlet() + ": " + f.text()).collect(Collectors.joining("\n"));
    }

    private static List<Long> validCitations(CitedSentence sentence, Map<Long, String> outletByArticle) {

        if (sentence == null || sentence.text() == null || sentence.text().isBlank() || sentence.articles() == null) {
            return List.of();
        }

        return sentence.articles().stream()
                .filter(outletByArticle::containsKey)
                .distinct()
                .toList();
    }

    private static String clip(String text) {
        String trimmed = text.trim();
        return trimmed.length() <= MAX_TEXT_CHARS ? trimmed : trimmed.substring(0, MAX_TEXT_CHARS);
    }

    private static <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
