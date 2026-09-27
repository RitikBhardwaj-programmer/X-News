package com.cfs.xnews.jev;

import com.cfs.xnews.jev.dto.JevRequest;
import com.cfs.xnews.jev.dto.JevResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JevClient {

    private static final Logger log = LoggerFactory.getLogger(JevClient.class);

    private final RestClient restClient;

    private final boolean configured;

    public JevClient(
            @Value("${ai.jev.url:https://api.typesafe.ai}")
            String baseUrl,

            @Value("${ai.jev.api-key:}")
            String apiKey
    ) {
        this.configured = apiKey != null && !apiKey.isBlank();

        if (!configured) {
            log.info("JEV_API_KEY not set: Jev is skipped and categories come from keyword rules");
        }

        this.restClient = RestClient
                .builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    /** False when no API key is configured; callers should not call evaluate(). */
    public boolean isConfigured() {
        return configured;
    }

    public JevResponse evaluate(JevRequest request) {

        return restClient
                .post()
                .uri("/v1/systemone")
                .body(request)
                .retrieve()
                .body(JevResponse.class);
    }
}
