package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EventSummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/events")
public class NewsEventController {

    NewsEventRepository eventRepository;
    private final NewsEventService newsEventService;
    public NewsEventController(NewsEventRepository eventRepository, NewsEventService newsEventService) {
        this.eventRepository = eventRepository;
        this.newsEventService = newsEventService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<NewsEvent>> getEvent(@PathVariable Long id){
        return ResponseEntity.ok(eventRepository.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<EventSummaryResponse>> getAllEvents() {

        return ResponseEntity.ok(
                newsEventService.getAllEvents()
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllEvents() {

        eventRepository.deleteAllInBatch();

        return ResponseEntity.noContent().build();
    }

}
