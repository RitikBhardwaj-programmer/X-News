package com.cfs.xnews.analysis;

import com.cfs.xnews.ai.AIService;
import com.cfs.xnews.ai.CitedAnalysis;
import com.cfs.xnews.ai.EventAIAnalysis;
import com.cfs.xnews.event.NewsEvent;
import com.cfs.xnews.event.NewsEventRepository;
import com.cfs.xnews.event.NewsEventService;
import com.cfs.xnews.news.articles.Article;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.cfs.xnews.provenance.ExtractionRunService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class EventAnalysisService {

    private final NewsEventRepository eventRepository;
    private final AIService aiService;
    private final ExtractionRunService extractionRunService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public EventAnalysisService(
            NewsEventRepository eventRepository,
            AIService aiService,
            ExtractionRunService extractionRunService
    ) {
        this.eventRepository = eventRepository;
        this.aiService = aiService;
        this.extractionRunService = extractionRunService;
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
        EventAIAnalysis raw =
                aiService.analyzeEvent(event);

        // Cite or drop: only sentences backed by this event's articles stay.
        Map<Long, String> outletByArticle = new HashMap<>();
        for (Article article : event.getArticles()) {
            outletByArticle.put(article.getId(), NewsEventService.outletOf(article.getSource()));
        }

        CitedAnalysis analysis = CitedAnalysis.validate(raw, outletByArticle);

        if (analysis.agreedFacts().isEmpty()) {
            throw new RuntimeException(
                    "The AI analysis cited none of this event's articles. Please try again."
            );
        }

        event.setAnalysis(toJson(analysis));

        event.setSummary(
                analysis.summaryText()
        );

        event.setBiasAnalysis(
                analysis.framingText()
        );

        event.setDisagreementLevel(
                analysis.disagreementLevel()
        );

        // Which model, prompt and code produced this analysis.
        event.setSummaryRunId(
                extractionRunService.runId(
                        "event-analysis",
                        aiService.modelName(),
                        AIService.PROMPT_VERSION
                )
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

    private String toJson(CitedAnalysis analysis) {

        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialise the analysis", e);
        }
    }
}