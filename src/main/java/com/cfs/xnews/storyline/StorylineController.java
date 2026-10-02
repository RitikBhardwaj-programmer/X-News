package com.cfs.xnews.storyline;

import com.cfs.xnews.event.NewsEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class StorylineController {

    static final int MAX_DEPTH = 2;
    static final int MAX_EVENTS = 30;

    private final EventRelationRepository relationRepository;
    private final NewsEventRepository eventRepository;

    public StorylineController(EventRelationRepository relationRepository, NewsEventRepository eventRepository) {
        this.relationRepository = relationRepository;
        this.eventRepository = eventRepository;
    }

    // depth 0 is the requested event; 1 and 2 are linked events.
    public record TimelineEntry(
            Long eventId,
            String title,
            LocalDateTime firstActivity,
            LocalDateTime lastActivity,
            int articleCount,
            int depth,
            boolean current
    ) {
    }

    @GetMapping("/api/v1/events/{id}/timeline")
    public ResponseEntity<List<TimelineEntry>> getTimeline(@PathVariable Long id) {

        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                relationRepository.findStoryline(id, MAX_DEPTH, MAX_EVENTS).stream()
                        .map(StorylineController::toEntry)
                        .toList()
        );
    }

    static TimelineEntry toEntry(Object[] row) {

        int depth = ((Number) row[5]).intValue();

        return new TimelineEntry(
                ((Number) row[0]).longValue(),
                (String) row[1],
                toLocalDateTime(row[2]),
                toLocalDateTime(row[3]),
                ((Number) row[4]).intValue(),
                depth,
                depth == 0
        );
    }

    private static LocalDateTime toLocalDateTime(Object value) {

        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }

        return (LocalDateTime) value;
    }
}
