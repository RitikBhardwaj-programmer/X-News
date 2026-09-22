package com.cfs.xnews.jev;

import com.cfs.xnews.jev.dto.JevRequest;
import com.cfs.xnews.jev.dto.JevResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class JevClient {

    private final RestClient restClient;

    public JevClient(
            @Value("${ai.jev.url:https://api.typesafe.ai}")
            String baseUrl,

            @Value("${ai.jev.api-key}")
            String apiKey
    ) {
        this.restClient = RestClient
                .builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
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
