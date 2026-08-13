package com.cfs.xnews.news.articles.dto;



import com.cfs.xnews.news.articles.Article;

import java.time.LocalDateTime;

public record ArticleResponse(
        Long id,
        String title,
        String description,
        String url,
        String source,
        LocalDateTime publishedAt
) {

    public static ArticleResponse from(Article article) {

        return new ArticleResponse(
                article.getId(),
                article.getTitle(),
                article.getDescription(),
                article.getUrl(),
                article.getSource(),
                article.getPublishedAt()
        );
    }
}
