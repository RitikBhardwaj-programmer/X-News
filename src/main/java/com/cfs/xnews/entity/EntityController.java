package com.cfs.xnews.entity;

import com.cfs.xnews.event.NewsEventRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class EntityController {

    static final int MAX_ENTITIES = 20;

    private final NamedEntityRepository entityRepository;
    private final NewsEventRepository eventRepository;

    public EntityController(NamedEntityRepository entityRepository, NewsEventRepository eventRepository) {
        this.entityRepository = entityRepository;
        this.eventRepository = eventRepository;
    }

    public record EventEntity(Long entityId, String name, String type, long articleCount) {
    }

    @GetMapping("/api/v1/events/{id}/entities")
    public ResponseEntity<List<EventEntity>> getEventEntities(@PathVariable Long id) {

        if (!eventRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(
                entityRepository.findForEvent(id, MAX_ENTITIES).stream()
                        .map(EntityController::toEventEntity)
                        .toList()
        );
    }

    static EventEntity toEventEntity(Object[] row) {

        return new EventEntity(
                ((Number) row[0]).longValue(),
                (String) row[1],
                (String) row[2],
                ((Number) row[3]).longValue()
        );
    }
}
