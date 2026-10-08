package com.cfs.xnews.title;

import com.cfs.xnews.ai.EventAITitle;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * "Cite or drop" for event titles: a model title is kept only if it is
 * short and cites at least two distinct articles of the event. Anything
 * else is discarded and the event keeps its first headline.
 */
public record GeneratedTitle(String text, List<Long> articles) {

    static final int MAX_CHARS = 120;
    static final int MIN_CITED_ARTICLES = 2;

    public static Optional<GeneratedTitle> validate(EventAITitle raw, Set<Long> eventArticleIds) {

        if (raw == null || raw.title() == null || raw.articles() == null) {
            return Optional.empty();
        }

        String text = clean(raw.title());

        if (text.isEmpty() || text.length() > MAX_CHARS) {
            return Optional.empty();
        }

        List<Long> cited = raw.articles().stream()
                .filter(Objects::nonNull)
                .filter(eventArticleIds::contains)
                .distinct()
                .toList();

        if (cited.size() < MIN_CITED_ARTICLES) {
            return Optional.empty();
        }

        return Optional.of(new GeneratedTitle(text, cited));
    }

    // Collapses whitespace and drops wrapping quotes. A trailing full stop is
    // kept: it may end an abbreviation ("U.S.").
    static String clean(String title) {

        String text = title.replaceAll("\\s+", " ").trim();

        while (text.length() >= 2 && isQuote(text.charAt(0)) && isQuote(text.charAt(text.length() - 1))) {
            text = text.substring(1, text.length() - 1).trim();
        }

        return text;
    }

    private static boolean isQuote(char c) {
        return c == '"' || c == '\'' || c == '“' || c == '”' || c == '‘' || c == '’';
    }
}
