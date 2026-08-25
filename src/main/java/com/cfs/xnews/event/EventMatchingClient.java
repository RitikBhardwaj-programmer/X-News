package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EmbeddingRequest;
import com.cfs.xnews.event.dto.EmbeddingResponse;
import com.cfs.xnews.event.dto.EventMatchRequest;
import com.cfs.xnews.event.dto.EventMatchResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EventMatchingClient {

    private final RestClient restClient;

    public EventMatchingClient(
            @Value("${ai.event-matcher.url}")
            String aiServiceUrl
    ) {
        this.restClient = RestClient
                .builder()
                .baseUrl(aiServiceUrl)
                .build();
    }

    public EmbeddingResponse generateEmbedding(
            String text
    ) {

        return restClient
                .post()
                .uri("/embed")
                .body(
                        new EmbeddingRequest(text)
                )
                .retrieve()
                .body(EmbeddingResponse.class);
    }

    public EventMatchResponse predict(
            EventMatchRequest request
    ) {

        return restClient
                .post()
                .uri("/predict")
                .body(request)
                .retrieve()
                .body(EventMatchResponse.class);
    }
}