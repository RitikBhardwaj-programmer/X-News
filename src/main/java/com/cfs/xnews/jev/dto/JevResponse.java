package com.cfs.xnews.jev.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record JevResponse(
        String model,
        Map<String, JevAnswer> answers,
        Usage usage
) {

    public record Usage(
            @JsonProperty("input_tokens")
            int inputTokens,

            @JsonProperty("output_tokens")
            int outputTokens
    ) {
    }
}
