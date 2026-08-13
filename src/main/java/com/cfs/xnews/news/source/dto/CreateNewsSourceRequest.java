package com.cfs.xnews.news.source.dto;


import com.cfs.xnews.news.source.NewsSourceType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateNewsSourceRequest(

        @NotBlank(message = "Source name is required")
        String name,

        @NotBlank(message = "Source URL is required")
        String url,

        @NotNull(message = "Source type is required")
        NewsSourceType type
) {
}
