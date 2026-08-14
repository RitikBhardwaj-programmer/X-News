package com.cfs.xnews.processing.processor;


import com.cfs.xnews.news.articles.Article;
import org.springframework.stereotype.Component;

@Component
public class CategoryProcessor {

    public String classify(Article article) {

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

    private boolean containsAny(
            String text,
            String... keywords
    ) {

        for (String keyword : keywords) {

            if (text.contains(keyword)) {
                return true;
            }
        }

        return false;
    }
}