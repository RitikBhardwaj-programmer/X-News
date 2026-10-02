package com.cfs.xnews.provenance;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtractionRunServiceTest {

    private final ExtractionRunRepository repository = mock(ExtractionRunRepository.class);
    private final ExtractionRunService service = new ExtractionRunService(repository, "sha-abc1234");

    @Test
    void createsTheRunOnceThenServesItFromCache() {

        when(repository.findIdByConfiguration("event-analysis", "gemini-3.6-flash", "p1", "sha-abc1234"))
                .thenReturn(Optional.of(7L));

        assertThat(service.runId("event-analysis", "gemini-3.6-flash", "p1")).isEqualTo(7L);
        assertThat(service.runId("event-analysis", "gemini-3.6-flash", "p1")).isEqualTo(7L);

        verify(repository, times(1)).insertIfAbsent("event-analysis", "gemini-3.6-flash", "p1", "sha-abc1234");
    }

    @Test
    void differentConfigurationsAreDifferentRuns() {

        when(repository.findIdByConfiguration(eq("event-matcher"), eq("v2/default"), anyString(), anyString()))
                .thenReturn(Optional.of(1L));
        when(repository.findIdByConfiguration(eq("event-matcher"), eq("v2/2026-10-03T02:00:04Z/11480"), anyString(), anyString()))
                .thenReturn(Optional.of(2L));

        assertThat(service.runId("event-matcher", "v2/default", ExtractionRunService.NO_PROMPT)).isEqualTo(1L);
        assertThat(service.runId("event-matcher", "v2/2026-10-03T02:00:04Z/11480", ExtractionRunService.NO_PROMPT))
                .isEqualTo(2L);
    }

    @Test
    void blankModelsAreRecordedAsUnknownAndLongOnesAreCut() {

        when(repository.findIdByConfiguration(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(3L));

        service.runId("event-analysis", " ", "p1");
        verify(repository).insertIfAbsent("event-analysis", "unknown", "p1", "sha-abc1234");

        service.runId("event-matcher", "m".repeat(200), "n/a");
        verify(repository).insertIfAbsent("event-matcher", "m".repeat(150), "n/a", "sha-abc1234");
    }

    @Test
    void aMissingRowAfterInsertIsAnError() {

        when(repository.findIdByConfiguration(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.runId("event-analysis", "m", "p"))
                .isInstanceOf(IllegalStateException.class);
    }
}
