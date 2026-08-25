package com.cfs.xnews.event;

import com.cfs.xnews.news.articles.Article;
import org.springframework.stereotype.Service;

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
}