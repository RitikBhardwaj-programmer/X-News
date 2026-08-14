package com.cfs.xnews.processing.processor;

import org.springframework.stereotype.Component;

@Component
public class ContentCleaner {

    public String clean(String content) {

        if (content == null) {
            return null;
        }

        return content
                .replaceAll("<[^>]*>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}