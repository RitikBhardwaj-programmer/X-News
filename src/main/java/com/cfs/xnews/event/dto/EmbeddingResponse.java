package com.cfs.xnews.event.dto;

import java.util.List;

public record EmbeddingResponse(
        List<Double> embedding
) {
}