package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EventMatchResult(
        @JsonProperty("event_id")
        Long eventId,

        double probability,

        double similarity
) {
}
