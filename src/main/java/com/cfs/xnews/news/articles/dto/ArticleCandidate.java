package com.cfs.xnews.news.articles.dto;

import com.cfs.xnews.news.articles.Article;

public record ArticleCandidate(
        Article article,
        double similarity
) {
}
