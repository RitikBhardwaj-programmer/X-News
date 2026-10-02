package com.cfs.xnews.search;

import java.time.LocalDateTime;
import java.util.List;

// mode is "hybrid" (full text fused with embeddings) or "text" when the
// query couldn't be embedded, so a client can tell how results were ranked.
public record SearchResponse(
        String query,
        String mode,
        List<Result> results
) {

    public record Result(
            Long articleId,
            String title,
            String source,
            String url,
            LocalDateTime publishedAt,
            Long eventId,
            double score
    ) {
    }
}
