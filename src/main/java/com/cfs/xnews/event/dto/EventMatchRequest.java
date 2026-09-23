package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EventMatchRequest(
        @JsonProperty("article_embedding")
        float[] articleEmbedding,

        List<EventCandidate> candidates
) {
}
