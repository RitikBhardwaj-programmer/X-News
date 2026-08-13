package com.cfs.xnews.news.source.dto;



import com.cfs.xnews.news.source.NewsSource;
import com.cfs.xnews.news.source.NewsSourceType;

import java.time.LocalDateTime;

public record NewsSourceResponse(

        Long id,
        String name,
        String url,
        NewsSourceType type,
        boolean enabled,
        LocalDateTime createdAt

) {

    public static NewsSourceResponse from(
            NewsSource source
    ) {

        return new NewsSourceResponse(
                source.getId(),
                source.getName(),
                source.getUrl(),
                source.getType(),
                source.isEnabled(),
                source.getCreatedAt()
        );
    }
}