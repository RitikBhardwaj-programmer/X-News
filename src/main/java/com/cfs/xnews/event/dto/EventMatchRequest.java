package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EventMatchRequest(
        double similarity,

        @JsonProperty("temporal_score")
        double temporalScore
) {
}