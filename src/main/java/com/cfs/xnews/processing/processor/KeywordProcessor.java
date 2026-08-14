package com.cfs.xnews.processing.processor;


import com.cfs.xnews.news.articles.Article;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class KeywordProcessor {

    private static final Set<String> STOP_WORDS =
            Set.of(
                    "the", "is", "a", "an", "and",
                    "or", "of", "to", "in", "on",
                    "for", "with", "this", "that",
                    "from", "by", "as", "at",
                    "be", "are", "was", "were",
                    "has", "have", "had", "it",
                    "its", "will", "would", "can",
                    "could", "about", "after",
                    "before", "into", "over",
                    "their", "they", "them"
            );

    public String extract(Article article) {

        String text =
                (article.getTitle() + " " +
                        article.getDescription())
                        .toLowerCase();

        Map<String, Long> frequencies =
                Arrays.stream(
                                text.replaceAll(
                                        "[^a-zA-Z ]",
                                        " "
                                ).split("\\s+")
                        )
                        .filter(word ->
                                word.length() > 2
                        )
                        .filter(word ->
                                !STOP_WORDS.contains(word)
                        )
                        .collect(
                                Collectors.groupingBy(
                                        word -> word,
                                        Collectors.counting()
                                )
                        );

        return frequencies.entrySet()
                .stream()
                .sorted(
                        Map.Entry
                                .<String, Long>comparingByValue()
                                .reversed()
                )
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(
                        Collectors.joining(", ")
                );
    }
}