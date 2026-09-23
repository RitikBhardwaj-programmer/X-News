package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EventCandidate(
        @JsonProperty("event_id")
        Long eventId,

        @JsonProperty("centroid_embedding")
        float[] centroidEmbedding,

        @JsonProperty("temporal_score")
        double temporalScore
) {
}
