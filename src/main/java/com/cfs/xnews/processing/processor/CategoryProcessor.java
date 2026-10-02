package com.cfs.xnews.processing.processor;


import com.cfs.xnews.jev.JevClient;
import com.cfs.xnews.jev.dto.JevAnswer;
import com.cfs.xnews.jev.dto.JevQuestion;
import com.cfs.xnews.jev.dto.JevRequest;
import com.cfs.xnews.jev.dto.JevResponse;
import com.cfs.xnews.news.articles.Article;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
public class CategoryProcessor {

    private static final Logger log = LoggerFactory.getLogger(CategoryProcessor.class);

    private static final Map<String, String> CATEGORIES = Map.of(
            "AI", "Artificial intelligence, machine learning, LLMs",
            "TECHNOLOGY", "Software, hardware, cloud computing, cybersecurity",
            "BUSINESS", "Markets, economy, finance, corporate news",
            "POLITICS", "Government, elections, policy, world leaders",
            "SPORTS", "Sports and athletic competition",
            "SCIENCE", "Scientific research, space, physics",
            "OTHER", "Doesn't clearly fit any other category"
    );

    private final JevClient jevClient;

    public CategoryProcessor(JevClient jevClient) {
        this.jevClient = jevClient;
    }

    public String classify(Article article) {

        if (!jevClient.isConfigured()) {
            return classifyByKeyword(article);
        }

        try {

            return classifyWithJev(article);

        } catch (Exception e) {

            log.warn(
                    "Jev classification failed, falling back to keyword matching: {}",
                    e.getMessage()
            );

            return classifyByKeyword(article);
        }
    }

    private String classifyWithJev(Article article) {

        String text =
                article.getTitle() + " " +
                        article.getDescription();

        JevRequest request = JevRequest.of(
                text,
                Map.of(
                        "category",
                        JevQuestion.choice(
                                "Which category best fits this news article?",
                                CATEGORIES
                        )
                )
        );

        JevResponse response = jevClient.evaluate(request);

        JevAnswer answer =
                response.answers().get("category");

        if (answer == null || answer.choice() == null) {

            throw new IllegalStateException(
                    "Jev returned no category choice"
            );
        }

        return answer.choice();
    }

    private String classifyByKeyword(Article article) {

        String text = (
                article.getTitle() + " " +
                        article.getDescription()
        ).toLowerCase();

        if (containsAny(text,
                "artificial intelligence",
                "ai",
                "machine learning",
                "llm",
                "openai",
                "google ai",
                "microsoft ai")) {

            return "AI";
        }

        if (containsAny(text,
                "technology",
                "software",
                "computer",
                "cloud",
                "cybersecurity")) {

            return "TECHNOLOGY";
        }

        if (containsAny(text,
                "stock",
                "market",
                "economy",
                "finance",
                "business")) {

            return "BUSINESS";
        }

        if (containsAny(text,
                "government",
                "minister",
                "election",
                "president",
                "parliament")) {

            return "POLITICS";
        }

        if (containsAny(text,
                "football",
                "cricket",
                "tennis",
                "olympics",
                "sport")) {

            return "SPORTS";
        }

        if (containsAny(text,
                "science",
                "research",
                "space",
                "nasa",
                "physics")) {

            return "SCIENCE";
        }

        return "OTHER";
    }

    // Whole words only (an optional plural "s"/"es" is allowed): a plain
    // substring match made "ai" fire on "said", "rain" and "Chennai", so
    // most articles were classed as AI.
    private static final Map<String, Pattern> KEYWORD_PATTERNS = new ConcurrentHashMap<>();

    private boolean containsAny(
            String text,
            String... keywords
    ) {

        for (String keyword : keywords) {

            Pattern pattern = KEYWORD_PATTERNS.computeIfAbsent(
                    keyword,
                    k -> Pattern.compile("\\b" + Pattern.quote(k) + "(?:e?s)?\\b")
            );

            if (pattern.matcher(text).find()) {
                return true;
            }
        }

        return false;
    }
}