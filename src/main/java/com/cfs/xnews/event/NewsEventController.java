package com.cfs.xnews.event;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/events")
public class NewsEventController {

    NewsEventRepository eventRepository;

    public NewsEventController(NewsEventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Optional<NewsEvent>> getEvent(@PathVariable Long id){
        return ResponseEntity.ok(eventRepository.findById(id));
    }

    @GetMapping
    public ResponseEntity<List<NewsEvent>> getAllEvents() {
        return ResponseEntity.ok(
                eventRepository.findAll()
        );
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteAllEvents() {

        eventRepository.deleteAllInBatch();

        return ResponseEntity.noContent().build();
    }

}
