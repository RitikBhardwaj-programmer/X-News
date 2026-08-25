package com.cfs.xnews.event.dto;

import java.util.Map;

public record EventMatchResponse(
        double probability,
        String prediction,
        Map<String, Double> features
) {}