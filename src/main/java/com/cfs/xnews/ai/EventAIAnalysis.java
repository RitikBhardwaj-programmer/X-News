package com.cfs.xnews.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// No misinformation risk: a single LLM-generated number with no evidence
// behind it (information-layer spec section 13-14). Unknown fields are
// ignored, so a model reply that still includes it doesn't break parsing.
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventAIAnalysis(
        String summary,
        String biasAnalysis,
        String disagreementLevel
) {
}
