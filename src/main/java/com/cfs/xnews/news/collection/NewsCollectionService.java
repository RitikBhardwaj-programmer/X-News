package com.cfs.xnews.news.collection;



import com.cfs.xnews.news.articles.Article;
import com.cfs.xnews.news.articles.ArticleRepository;
import com.cfs.xnews.news.collection.dto.CollectedArticle;
import com.cfs.xnews.news.source.NewsSource;
import com.cfs.xnews.news.source.NewsSourceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NewsCollectionService {

    private final NewsSourceRepository sourceRepository;
    private final ArticleRepository articleRepository;
    private final NewsSourceClient newsSourceClient;

    public NewsCollectionService(
            NewsSourceRepository sourceRepository,
            ArticleRepository articleRepository,
            NewsSourceClient newsSourceClient
    ) {
        this.sourceRepository = sourceRepository;
        this.articleRepository = articleRepository;
        this.newsSourceClient = newsSourceClient;
    }

    @Transactional
    public int collectFromSource(Long sourceId) {

        NewsSource source = sourceRepository
                .findById(sourceId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "News source not found"
                        )
                );

        if (!source.isEnabled()) {
            throw new RuntimeException(
                    "News source is disabled"
            );
        }

        List<CollectedArticle> collectedArticles =
                newsSourceClient.fetchArticles(source);

        int savedCount = 0;

        for (CollectedArticle collected :
                collectedArticles) {

            if (collected.url() == null ||
                    collected.url().isBlank()) {
                continue;
            }

            if (articleRepository
                    .existsByUrl(collected.url())) {
                continue;
            }

            Article article = new Article(
                    collected.title(),
                    collected.description(),
                    collected.url(),
                    collected.source(),
                    collected.publishedAt()
            );

            articleRepository.save(article);

            savedCount++;
        }

        return savedCount;
    }
}
