package com.cfs.xnews.processing.processor;


import com.cfs.xnews.news.articles.Article;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SentimentProcessor {

    private static final Set<String> POSITIVE_WORDS = Set.of(
            "success",
            "successful",
            "growth",
            "grow",
            "positive",
            "improve",
            "improved",
            "benefit",
            "benefits",
            "win",
            "winner",
            "breakthrough",
            "progress",
            "strong",
            "boost",
            "increase"
    );

    private static final Set<String> NEGATIVE_WORDS = Set.of(
            "failure",
            "failed",
            "crisis",
            "crash",
            "decline",
            "declined",
            "negative",
            "loss",
            "losses",
            "attack",
            "dead",
            "death",
            "war",
            "threat",
            "risk",
            "fall",
            "fell",
            "decrease"
    );

    public String analyze(Article article) {

        String text =
                (article.getTitle() + " " +
                        article.getDescription())
                        .toLowerCase();

        String[] words =
                text.replaceAll("[^a-zA-Z ]", " ")
                        .split("\\s+");

        int positiveScore = 0;
        int negativeScore = 0;

        for (String word : words) {

            if (POSITIVE_WORDS.contains(word)) {
                positiveScore++;
            }

            if (NEGATIVE_WORDS.contains(word)) {
                negativeScore++;
            }
        }

        if (positiveScore > negativeScore) {
            return "POSITIVE";
        }

        if (negativeScore > positiveScore) {
            return "NEGATIVE";
        }

        return "NEUTRAL";
    }
}