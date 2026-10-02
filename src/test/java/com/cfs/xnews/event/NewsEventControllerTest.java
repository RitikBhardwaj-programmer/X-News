package com.cfs.xnews.event;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NewsEventControllerTest {

    private final NewsEventRepository repository = mock(NewsEventRepository.class);
    private final NewsEventController controller =
            new NewsEventController(repository, mock(NewsEventService.class));

    @Test
    void unknownEventIsNotFoundInsteadOfAnEmptyOk() {

        when(repository.findById(404L)).thenReturn(Optional.empty());

        ResponseEntity<NewsEvent> response = controller.getEvent(404L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void knownEventIsReturned() {

        NewsEvent event = new NewsEvent("title", "description");
        when(repository.findById(1L)).thenReturn(Optional.of(event));

        ResponseEntity<NewsEvent> response = controller.getEvent(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(event);
    }
}
