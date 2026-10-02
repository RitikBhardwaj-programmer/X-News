package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// POST /entities on the AI service (V4 roadmap step 3).
public record EntityExtractionResponse(
        @JsonProperty("extractor_version")
        String extractorVersion,

        List<Mention> mentions
) {

    // type: team | name; field: title | description
    public record Mention(
            String text,
            String normalized,
            String type,
            String field
    ) {
    }
}
