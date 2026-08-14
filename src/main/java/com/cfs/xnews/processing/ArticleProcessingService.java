package com.cfs.xnews.processing;


import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventService;
import com.cfs.xnews.kafka.ArticleEvent;
import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.processing.processor.CategoryProcessor;
import com.cfs.xnews.processing.processor.ContentCleaner;

import com.cfs.xnews.processing.processor.KeywordProcessor;
import com.cfs.xnews.processing.processor.SentimentProcessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ArticleProcessingService {

    private final NewsEventService newsEventService;
    private final SentimentProcessor sentimentProcessor;
    private final CategoryProcessor categoryProcessor;
    private final KeywordProcessor keywordProcessor;
    private final ArticleRepository articleRepository;
    private final ContentCleaner contentCleaner;

    public ArticleProcessingService(
            NewsEventService newsEventService, SentimentProcessor sentimentProcessor, CategoryProcessor categoryProcessor, KeywordProcessor keywordProcessor, ArticleRepository articleRepository,
            ContentCleaner contentCleaner
    ) {
        this.newsEventService = newsEventService;
        this.sentimentProcessor = sentimentProcessor;
        this.categoryProcessor = categoryProcessor;
        this.keywordProcessor = keywordProcessor;
        this.articleRepository = articleRepository;
        this.contentCleaner = contentCleaner;
    }

    @Transactional
    public void process(ArticleEvent event) {

        Article article = articleRepository
                .findById(event.articleId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Article not found: " +
                                        event.articleId()
                        )
                );

        // Find existing event or create a new one
        NewsEvent newsEvent =
                newsEventService.findOrCreateEvent(article);

        // Clean content
        String cleanedDescription =
                contentCleaner.clean(
                        article.getDescription()
                );

        article.setDescription(cleanedDescription);

        // Category
        String category =
                categoryProcessor.classify(article);

        article.setCategory(category);

        // Keywords
        String keywords =
                keywordProcessor.extract(article);

        article.setKeywords(keywords);

        // Sentiment
        String sentiment =
                sentimentProcessor.analyze(article);

        article.setSentiment(sentiment);

        // Save processed article
        articleRepository.save(article);

        System.out.println(
                "Processed article: " +
                        article.getTitle() +
                        " | Event: " +
                        newsEvent.getId() +
                        " | Category: " +
                        category +
                        " | Sentiment: " +
                        sentiment +
                        " | Keywords: " +
                        keywords
        );
    }
}