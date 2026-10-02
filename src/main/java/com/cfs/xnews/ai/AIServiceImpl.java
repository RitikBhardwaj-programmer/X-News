package com.cfs.xnews.ai;

import com.cfs.xnews.event.NewsEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.types.GenerateContentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger log = LoggerFactory.getLogger(AIServiceImpl.class);

    // Overloaded (503), rate limited (429) or other transient server errors.
    private static final Set<Integer> RETRYABLE_CODES = Set.of(429, 500, 502, 503, 504);

    // Wait before each retry; package-private so tests can skip the waiting.
    long[] retryDelaysMs = {1_000, 2_000};

    private final Client client;
    private final ObjectMapper objectMapper;

    @Value("${gemini.model}")
    private String model;

    public AIServiceImpl(
            @Value("${gemini.api-key}") String apiKey
    ) {
        this.client = Client.builder()
                .apiKey(apiKey)
                .build();

        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public EventAIAnalysis analyzeEvent(NewsEvent event) {

        String prompt = """
                You are a neutral news analysis system.

                The articles below cover the SAME news event. Each has an ID.

                Return ONLY valid JSON. Do not use markdown.
                Do not wrap the JSON in ```.

                Required format:

                {
                  "agreedFacts": [
                    {"text": "One factual sentence the sources agree on.", "articles": [101, 102]}
                  ],
                  "framing": [
                    {"outlet": "Outlet name", "text": "One sentence on how this outlet frames the story.", "articles": [101]}
                  ],
                  "disagreementLevel": "LOW"
                }

                Rules:
                - Every sentence must cite, in "articles", the IDs of the
                  articles that state it. A sentence without a supporting
                  article ID must be left out.
                - Use only what the articles say. No outside knowledge.
                - agreedFacts: 2 to 5 short sentences that the cited
                  articles agree on. If sources conflict on a point, it is
                  not an agreed fact.
                - framing: at most one sentence per outlet, describing its
                  emphasis or angle, citing only that outlet's articles.
                - disagreementLevel is exactly LOW (sources substantially
                  agree), MEDIUM (meaningful differences in framing or
                  claims) or HIGH (major conflicting claims).
                - Do not claim something is false because sources disagree.
                  This is not fact verification.

                EVENT:
                %s

                ARTICLES:
                %s
                """.formatted(
                event.getTitle(),
                buildArticlesText(event)
        );

        GenerateContentResponse response =
                generateWithRetry(prompt);

        String json = cleanJson(response.text());

        try {

            return objectMapper.readValue(
                    json,
                    EventAIAnalysis.class
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "Failed to parse Gemini response: " + json,
                    e
            );
        }
    }

    private GenerateContentResponse generateWithRetry(String prompt) {

        for (int attempt = 0; ; attempt++) {

            try {
                return callGemini(prompt);

            } catch (ApiException e) {

                if (!RETRYABLE_CODES.contains(e.code())) {
                    throw e;
                }

                if (attempt >= retryDelaysMs.length) {
                    throw new AIServiceUnavailableException(
                            "The AI service is busy right now. Please try again in a minute.",
                            e
                    );
                }

                log.warn(
                        "Gemini call failed with {} (attempt {} of {}), retrying in {} ms",
                        e.code(),
                        attempt + 1,
                        retryDelaysMs.length + 1,
                        retryDelaysMs[attempt]
                );

                sleep(retryDelaysMs[attempt]);
            }
        }
    }

    // Seam for tests: the only place that talks to Gemini.
    GenerateContentResponse callGemini(String prompt) {

        return client.models.generateContent(
                model,
                prompt,
                null
        );
    }

    private static void sleep(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AIServiceUnavailableException(
                    "Interrupted while waiting to retry the AI service",
                    e
            );
        }
    }

    private String buildArticlesText(
            NewsEvent event
    ) {

        StringBuilder builder =
                new StringBuilder();

        event.getArticles().forEach(article -> {

            builder.append("\n--- ARTICLE ---\n");

            builder.append("ID: ")
                    .append(article.getId())
                    .append("\n");

            builder.append("SOURCE: ")
                    .append(article.getSource())
                    .append("\n");

            builder.append("TITLE: ")
                    .append(article.getTitle())
                    .append("\n");

            builder.append("DESCRIPTION: ")
                    .append(article.getDescription())
                    .append("\n");
        });

        return builder.toString();
    }

    private String cleanJson(String response) {

        if (response == null) {
            throw new RuntimeException(
                    "Gemini returned empty response"
            );
        }

        response = response.trim();

        if (response.startsWith("```json")) {
            response = response.substring(7);
        } else if (response.startsWith("```")) {
            response = response.substring(3);
        }

        if (response.endsWith("```")) {
            response = response.substring(
                    0,
                    response.length() - 3
            );
        }

        return response.trim();
    }
}