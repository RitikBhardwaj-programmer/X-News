package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// POST /claims on the AI service (V4 roadmap step 6).
public record ClaimExtractionResponse(
        @JsonProperty("extractor_version")
        String extractorVersion,

        List<Claim> claims
) {

    public record Claim(
            String subject,

            @JsonProperty("subject_normalized")
            String subjectNormalized,

            String predicate,

            double value,

            @JsonProperty("value_text")
            String valueText,

            String unit,

            String quote,

            int start,

            int end,

            String field
    ) {
    }
}
