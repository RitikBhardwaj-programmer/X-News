package com.cfs.xnews.event.dto;

// Title and description of one article, as the v2 matcher's TF-IDF sees it.
public record ArticleText(
        String title,
        String description
) {
}
