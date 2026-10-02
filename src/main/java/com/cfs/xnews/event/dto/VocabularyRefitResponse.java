package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record VocabularyRefitResponse(
        @JsonProperty("vocabulary_version")
        String vocabularyVersion,

        int documents
) {
}
