package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EventCoverageResponse;
import com.cfs.xnews.event.dto.EventMatchConfidence;
import com.cfs.xnews.event.dto.EventSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/events")
public class NewsEventController {

    private final NewsEventRepository eventRepository;
    private final NewsEventService newsEventService;
    public NewsEventController(NewsEventRepository eventRepository, NewsEventService newsEventService) {
        this.eventRepository = eventRepository;
        this.newsEventService = newsEventService;
    }

    // 404 for an unknown id, instead of 200 with a null body (which left the
    // event page blank).
    @GetMapping("/{id}")
    public ResponseEntity<NewsEvent> getEvent(@PathVariable Long id){
        return ResponseEntity.of(eventRepository.findById(id));
    }

    // Per-article match confidence: how each article came to be in the event.
    @GetMapping("/{id}/matches")
    public ResponseEntity<List<EventMatchConfidence>> getMatchConfidence(@PathVariable Long id) {

        return ResponseEntity.of(newsEventService.getMatchConfidence(id));
    }

    @GetMapping("/{id}/coverage")
    public ResponseEntity<EventCoverageResponse> getCoverage(@PathVariable Long id) {

        return ResponseEntity.of(newsEventService.getCoverage(id));
    }

    @GetMapping
    public ResponseEntity<List<EventSummaryResponse>> getAllEvents() {

        return ResponseEntity.ok(
                newsEventService.getAllEvents()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @PathVariable Long id
    ) {

        newsEventService.deleteEvent(id);

        return ResponseEntity.noContent().build();
    }

}
