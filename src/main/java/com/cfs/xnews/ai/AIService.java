package com.cfs.xnews.ai;

import com.cfs.xnews.event.NewsEvent;

public interface AIService {

    EventAIAnalysis analyzeEvent(NewsEvent event);
}