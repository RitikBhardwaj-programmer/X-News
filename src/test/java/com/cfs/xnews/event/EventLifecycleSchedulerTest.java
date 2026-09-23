package com.cfs.xnews.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventLifecycleSchedulerTest {

    @Mock
    private NewsEventRepository eventRepository;

    @Test
    void closesOpenEventsInactiveLongerThanTheConfiguredWindow() {

        when(eventRepository.closeEventsInactiveSince(any(), any(), any())).thenReturn(4);

        new EventLifecycleScheduler(eventRepository, 10).closeInactiveEvents();

        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);

        verify(eventRepository).closeEventsInactiveSince(
                org.mockito.ArgumentMatchers.eq(EventStatus.OPEN),
                org.mockito.ArgumentMatchers.eq(EventStatus.CLOSED),
                cutoff.capture()
        );

        Duration age = Duration.between(cutoff.getValue(), LocalDateTime.now());

        assertThat(age.toDays()).isEqualTo(10);
        assertThat(age.toHours()).isCloseTo(240L, within(1L));
    }
}
