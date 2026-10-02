package com.cfs.xnews.event.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Member similarities come from SQL over all members; members holds the
// newest members' texts, newest first.
public record EventCandidateV2(
        @JsonProperty("event_id")
        Long eventId,

        double similarity,

        @JsonProperty("temporal_score")
        double temporalScore,

        @JsonProperty("member_max")
        double memberMax,

        @JsonProperty("member_min")
        double memberMin,

        @JsonProperty("member_top3")
        double memberTop3,

        @JsonProperty("member_newest")
        double memberNewest,

        List<ArticleText> members
) {
}
