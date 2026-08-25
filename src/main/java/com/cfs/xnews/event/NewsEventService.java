package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EventSummaryResponse;
import com.cfs.xnews.news.articles.Article;
import org.springframework.stereotype.Service;

import java.util.List;

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
                        event.getSourceCount()
                ))
                .toList();
    }
}