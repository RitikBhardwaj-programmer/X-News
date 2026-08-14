package com.cfs.xnews.ai;

public record EventAIAnalysis(
        String summary,
        String biasAnalysis,
        String disagreementLevel,
        double misinformationRisk
) {
}