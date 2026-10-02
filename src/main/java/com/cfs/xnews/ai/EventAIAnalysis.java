package com.cfs.xnews.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

// The model's raw answer (V4 roadmap step 4): agreed facts and per-outlet
// framing, each sentence citing the article ids it rests on. It is checked
// by CitedAnalysis before anything is stored. Unknown fields are ignored, so
// an old-format reply doesn't break parsing (it then has no facts and fails
// validation).
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventAIAnalysis(
        List<CitedSentence> agreedFacts,
        List<CitedSentence> framing,
        String disagreementLevel
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CitedSentence(
            String outlet,
            String text,
            List<Long> articles
    ) {
    }
}
