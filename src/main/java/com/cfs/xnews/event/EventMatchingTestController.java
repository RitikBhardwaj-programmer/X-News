package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EventMatchRequest;
import com.cfs.xnews.event.dto.EventMatchResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-ai")
public class EventMatchingTestController {

    private final EventMatchingClient eventMatchingClient;

    public EventMatchingTestController(
            EventMatchingClient eventMatchingClient
    ) {
        this.eventMatchingClient = eventMatchingClient;
    }

    @PostMapping("/predict")
    public EventMatchResponse predict(
            @RequestBody EventMatchRequest request
    ) {

        return eventMatchingClient.predict(request);
    }
}