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
    public EventAIAnalysis analyzeEvent(NewsEvent event) {

        String prompt = """
                You are a neutral news analysis system.

                Analyze multiple articles covering the SAME news event.

                Return ONLY valid JSON.
                Do not use markdown.
                Do not wrap the JSON in ```.

                Required format:

                {
                  "summary": "3-4 sentence neutral summary",
                  "biasAnalysis": "cross-source framing and bias analysis",
                  "disagreementLevel": "LOW",
                  "misinformationRisk": 0.0
                }

                disagreementLevel must be exactly:
                LOW, MEDIUM, or HIGH.

                LOW:
                Sources substantially agree.

                MEDIUM:
                Sources have meaningful differences in framing or claims.

                HIGH:
                Sources make major conflicting claims about the event.

                misinformationRisk must be a number from 0.0 to 1.0.

                IMPORTANT:
                Do not claim that something is false merely because
                sources disagree.

                Do not treat this AI analysis as factual verification.

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