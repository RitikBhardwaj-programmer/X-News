package com.cfs.xnews.jev.dto;

import java.util.Map;

public record JevAnswer(
        String type,
        Double noul,
        String choice,
        Map<String, Double> probabilities,
        Double confidence,
        Double score,
        Map<String, String> legend
) {
}
