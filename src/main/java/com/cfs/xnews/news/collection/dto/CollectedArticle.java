package com.cfs.xnews.news.collection.dto;


import java.time.LocalDateTime;

public record CollectedArticle(
        String title,
        String description,
        String url,
        String source,
        LocalDateTime publishedAt
) {
}