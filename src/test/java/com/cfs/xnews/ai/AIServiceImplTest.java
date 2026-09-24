package com.cfs.xnews.ai;

import com.cfs.xnews.event.NewsEvent;
import com.google.genai.errors.ClientException;
import com.google.genai.errors.ServerException;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AIServiceImplTest {

    private static final String VALID_JSON = """
            {"summary":"s","biasAnalysis":"b","disagreementLevel":"LOW","misinformationRisk":0.1}
            """;

    /**
     * Replays a scripted sequence of Gemini outcomes: each entry is either a
     * RuntimeException to throw or a response text to return.
     */
    private static class ScriptedAIService extends AIServiceImpl {

        private final Deque<Object> script;
        int calls = 0;

        ScriptedAIService(Object... outcomes) {
            super("test-key");
            this.script = new ArrayDeque<>(List.of(outcomes));
            this.retryDelaysMs = new long[]{0, 0};
        }

        @Override
        GenerateContentResponse callGemini(String prompt) {
            calls++;
            Object next = script.removeFirst();
            if (next instanceof RuntimeException e) {
                throw e;
            }
            return GenerateContentResponse.builder()
                    .candidates(List.of(Candidate.builder()
                            .content(Content.fromParts(Part.fromText((String) next)))
                            .build()))
                    .build();
        }
    }

    private static ServerException overloaded() {
        return new ServerException(503, "UNAVAILABLE", "This model is currently experiencing high demand.");
    }

    private static NewsEvent event() {
        return new NewsEvent("title", "description");
    }

    @Test
    void analyzeEvent_retriesTransientFailuresThenSucceeds() {

        ScriptedAIService service = new ScriptedAIService(overloaded(), overloaded(), VALID_JSON);

        EventAIAnalysis analysis = service.analyzeEvent(event());

        assertThat(service.calls).isEqualTo(3);
        assertThat(analysis.disagreementLevel()).isEqualTo("LOW");
    }

    @Test
    void analyzeEvent_throwsUnavailableAfterRetriesAreExhausted() {

        ScriptedAIService service = new ScriptedAIService(overloaded(), overloaded(), overloaded());

        assertThatThrownBy(() -> service.analyzeEvent(event()))
                .isInstanceOf(AIServiceUnavailableException.class)
                .hasCauseInstanceOf(ServerException.class);

        assertThat(service.calls).isEqualTo(3);
    }

    @Test
    void analyzeEvent_doesNotRetryNonTransientClientErrors() {

        ClientException badRequest = new ClientException(400, "INVALID_ARGUMENT", "bad request");
        ScriptedAIService service = new ScriptedAIService(badRequest);

        assertThatThrownBy(() -> service.analyzeEvent(event()))
                .isSameAs(badRequest);

        assertThat(service.calls).isEqualTo(1);
    }
}
