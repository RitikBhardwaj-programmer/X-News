package com.cfs.xnews.event;

import com.cfs.xnews.news.articles.Article;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class NewsEventService {

    private final NewsEventRepository eventRepository;

    public NewsEventService(
            NewsEventRepository eventRepository
    ) {
        this.eventRepository = eventRepository;
    }

    public NewsEvent createEvent(Article article) {

        NewsEvent event = new NewsEvent(
                article.getTitle(),
                article.getDescription()
        );

        event.addArticle(article);

        return eventRepository.save(event);
    }
    private Set<String> tokenize(String text) {

        if (text == null) {
            return Set.of();
        }

        return new HashSet<>(
                Arrays.asList(
                        text.toLowerCase()
                                .replaceAll("[^a-zA-Z ]", " ")
                                .split("\\s+")
                )
        );
    }

    public double calculateSimilarity(
            String title1,
            String title2
    ) {

        Set<String> words1 = tokenize(title1);
        Set<String> words2 = tokenize(title2);

        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection =
                new HashSet<>(words1);

        intersection.retainAll(words2);

        Set<String> union =
                new HashSet<>(words1);

        union.addAll(words2);

        return (double) intersection.size()
                / union.size();
    }
    public NewsEvent findOrCreateEvent(Article article) {

        for (NewsEvent event : eventRepository.findAll()) {

            double similarity =
                    calculateSimilarity(
                            article.getTitle(),
                            event.getTitle()
                    );

            System.out.println(
                    "Comparing article with event: " +
                            event.getId() +
                            " | Similarity: " +
                            similarity
            );

            if (similarity >= 0.3) {
                event.addArticle(article);
                return event;
            }
        }

        return createEvent(article);
    }
}