package com.cfs.xnews.event.dto;

import java.util.List;

public record VocabularyRefitRequest(
        List<ArticleText> items
) {
}
