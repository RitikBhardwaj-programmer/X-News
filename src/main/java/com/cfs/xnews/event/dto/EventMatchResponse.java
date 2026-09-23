package com.cfs.xnews.event.dto;

import java.util.List;

public record EventMatchResponse(
        List<EventMatchResult> results
) {
}
