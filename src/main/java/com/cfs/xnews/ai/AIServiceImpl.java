package com.cfs.xnews.ai;

import com.cfs.xnews.event.NewsEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AIServiceImpl implements AIService {

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
                client.models.generateContent(
                        model,
                        prompt,
                        null
                );

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