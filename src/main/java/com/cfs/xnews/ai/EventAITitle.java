package com.cfs.xnews.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// The model's raw title and the article ids it rests on. It is checked by
// GeneratedTitle before anything is stored.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventAITitle(
        String title,
        List<Long> articles
) {
}
