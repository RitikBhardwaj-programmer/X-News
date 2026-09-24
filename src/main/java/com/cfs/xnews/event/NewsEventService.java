package com.cfs.xnews.event;

import com.cfs.xnews.analysis.FactCheckRepository;
import com.cfs.xnews.event.dto.EventSummaryResponse;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NewsEventService {

    private final NewsEventRepository eventRepository;
    private final ArticleRepository articleRepository;
    private final FactCheckRepository factCheckRepository;

    public NewsEventService(
            NewsEventRepository eventRepository,
            ArticleRepository articleRepository,
            FactCheckRepository factCheckRepository
    ) {
        this.eventRepository = eventRepository;
        this.articleRepository = articleRepository;
        this.factCheckRepository = factCheckRepository;
    }

    public NewsEvent createEvent(Article article) {

        NewsEvent event = new NewsEvent(
                article.getTitle(),
                article.getDescription()
        );

        event.addArticle(article);

        return eventRepository.save(event);
    }

    public List<EventSummaryResponse> getAllEvents() {

        return eventRepository
                .findAllEventSummaries()
                .stream()
                .map(event -> new EventSummaryResponse(
                        event.getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getSummary(),
                        event.getCreatedAt(),
                        event.getSourceCount(),
                        event.getVerificationStatus(),
                        event.getDisagreementLevel(),
                        event.getMisinformationRisk()
                ))
                .toList();
    }

    @Transactional
    public void deleteEvent(Long id) {

        NewsEvent event = eventRepository
                .findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Event not found"
                        )
                );

        // Articles outlive the event they were grouped under - detach
        // rather than delete, so their content isn't lost.
        for (Article article : event.getArticles()) {
            article.setNewsEvent(null);
        }

        articleRepository.saveAll(event.getArticles());

        // A fact-check has no meaning without the event it verifies.
        factCheckRepository.deleteAll(event.getFactChecks());

        eventRepository.delete(event);
    }
}