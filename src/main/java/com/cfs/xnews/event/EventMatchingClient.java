package com.cfs.xnews.event;

import com.cfs.xnews.event.dto.EmbeddingRequest;
import com.cfs.xnews.event.dto.EmbeddingResponse;
import com.cfs.xnews.event.dto.EventMatchRequest;
import com.cfs.xnews.event.dto.EventMatchResponse;
import com.cfs.xnews.event.dto.EventMatchV2Request;
import com.cfs.xnews.event.dto.EventMatchV2Response;
import com.cfs.xnews.event.dto.VocabularyRefitRequest;
import com.cfs.xnews.event.dto.VocabularyRefitResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Component
public class EventMatchingClient {

    // v2 runs next to v1 (shadow) or falls back to it (live), so a slow v2
    // call must give up instead of holding up article processing.
    static final Duration V2_PREDICT_TIMEOUT = Duration.ofSeconds(5);
    static final Duration VOCABULARY_REFIT_TIMEOUT = Duration.ofSeconds(60);

    private final RestClient restClient;
    private final RestClient v2PredictClient;
    private final RestClient vocabularyClient;

    public EventMatchingClient(
            @Value("${ai.event-matcher.url}")
            String aiServiceUrl,

            @Value("${ai.event-matcher.api-key}")
            String apiKey
    ) {
        this.restClient = RestClient
                .builder()
                .baseUrl(aiServiceUrl)
                .defaultHeader("X-API-Key", apiKey)
                .build();

        this.v2PredictClient = clientWithTimeout(aiServiceUrl, apiKey, V2_PREDICT_TIMEOUT);
        this.vocabularyClient = clientWithTimeout(aiServiceUrl, apiKey, VOCABULARY_REFIT_TIMEOUT);
    }

    private static RestClient clientWithTimeout(String url, String apiKey, Duration timeout) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeout);
        requestFactory.setReadTimeout(timeout);

        return RestClient
                .builder()
                .baseUrl(url)
                .defaultHeader("X-API-Key", apiKey)
                .requestFactory(requestFactory)
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

    public EventMatchV2Response predictV2(
            EventMatchV2Request request
    ) {

        return v2PredictClient
                .post()
                .uri("/predict/v2")
                .body(request)
                .retrieve()
                .body(EventMatchV2Response.class);
    }

    public VocabularyRefitResponse refitVocabulary(
            VocabularyRefitRequest request
    ) {

        return vocabularyClient
                .post()
                .uri("/vocabulary/v2")
                .body(request)
                .retrieve()
                .body(VocabularyRefitResponse.class);
    }
}