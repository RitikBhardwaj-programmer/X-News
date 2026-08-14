package com.cfs.xnews.analysis;

import com.cfs.xnews.ai.AIService;
import com.cfs.xnews.ai.EventAIAnalysis;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventAnalysisService {

    private final NewsEventRepository eventRepository;
    private final AIService aiService;

    public EventAnalysisService(
            NewsEventRepository eventRepository,
            AIService aiService
    ) {
        this.eventRepository = eventRepository;
        this.aiService = aiService;
    }

    @Transactional
    public NewsEvent analyze(Long eventId) {

        NewsEvent event =
                eventRepository.findById(eventId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Event not found"
                                )
                        );

        // ONE Gemini call
        EventAIAnalysis analysis =
                aiService.analyzeEvent(event);

        event.setSummary(
                analysis.summary()
        );

        event.setBiasAnalysis(
                analysis.biasAnalysis()
        );

        event.setDisagreementLevel(
                analysis.disagreementLevel()
        );

        event.setMisinformationRisk(
                analysis.misinformationRisk()
        );

        /*
         * We don't have trusted fact-checking evidence yet.
         * Therefore the event remains UNVERIFIED.
         */
        event.setVerificationStatus(
                "UNVERIFIED"
        );

        return eventRepository.save(event);
    }
}