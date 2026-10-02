package com.cfs.xnews.event.dto;

import java.time.LocalDateTime;

// How one article came to be in an event (V4 item 6, information-layer spec
// section 4: explicit uncertainty). decision is "joined" (attached to an
// existing event) or "started" (became the first article of a new event).
// probability is the matcher's estimate for its best candidate; it is an
// estimate, not a verdict.
public record EventMatchConfidence(
        Long articleId,
        String decision,
        String matcher,
        Double probability,
        Double threshold,
        String modelVersion,
        LocalDateTime decidedAt
) {
}
