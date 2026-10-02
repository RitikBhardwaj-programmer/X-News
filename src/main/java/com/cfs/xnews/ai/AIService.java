package com.cfs.xnews.ai;

import com.cfs.xnews.event.NewsEvent;

public interface AIService {

    // Bump when the analysis prompt changes, so summaries made with
    // different prompts can be told apart (provenance, roadmap step 1).
    String PROMPT_VERSION = "event-analysis-2026-10-02";

    EventAIAnalysis analyzeEvent(NewsEvent event);

    // The model that produced the analysis, e.g. gemini-3.6-flash.
    String modelName();
}