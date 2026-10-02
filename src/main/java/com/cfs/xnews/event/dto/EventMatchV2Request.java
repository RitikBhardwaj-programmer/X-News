package com.cfs.xnews.event.dto;

import java.util.List;

public record EventMatchV2Request(
        String title,
        String description,
        List<EventCandidateV2> candidates
) {
}
