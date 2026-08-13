package com.cfs.xnews.kafka;


import java.time.LocalDateTime;

public record ArticleEvent(
        Long articleId,
        String title,
        String description,
        String url,
        String source,
        LocalDateTime publishedAt
) {
}