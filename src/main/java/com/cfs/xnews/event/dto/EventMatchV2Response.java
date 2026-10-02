package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// vocabularyVersion is "default" until the AI service's first vocabulary
// refit after it starts.
public record EventMatchV2Response(
        @JsonProperty("model_version")
        String modelVersion,

        @JsonProperty("vocabulary_version")
        String vocabularyVersion,

        double threshold,

        List<EventMatchV2Result> results
) {
}
