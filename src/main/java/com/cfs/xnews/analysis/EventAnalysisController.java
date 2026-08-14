package com.cfs.xnews.analysis;

import com.cfs.xnews.event.NewsEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/events")
public class EventAnalysisController {

    private final EventAnalysisService analysisService;

    public EventAnalysisController(
            EventAnalysisService analysisService
    ) {
        this.analysisService = analysisService;
    }

    @PostMapping("/{eventId}/analyze")
    public ResponseEntity<NewsEvent> analyzeEvent(
            @PathVariable Long eventId
    ) {

        return ResponseEntity.ok(
                analysisService.analyze(eventId)
        );
    }
}