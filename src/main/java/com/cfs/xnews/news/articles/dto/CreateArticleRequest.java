package com.cfs.xnews.news.articles.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateArticleRequest(

        @NotBlank
        String title,

        String description,

        @NotBlank
        String url,

        @NotBlank
        String source,

        @NotNull
        LocalDateTime publishedAt
) {
}
