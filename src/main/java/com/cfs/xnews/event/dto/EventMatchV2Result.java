package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record EventMatchV2Result(
        @JsonProperty("event_id")
        Long eventId,

        double probability,

        Map<String, Double> features
) {
}
