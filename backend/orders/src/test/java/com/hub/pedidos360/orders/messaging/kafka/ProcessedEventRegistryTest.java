package com.hub.pedidos360.orders.messaging.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProcessedEventRegistryTest {

    private static final String EVENT_ID = "evt-1";

    private ProcessedKafkaEventRepository repository;
    private ProcessedEventRegistry registry;

    @BeforeEach
    void setUp() {
        repository = mock(ProcessedKafkaEventRepository.class);
        registry = new ProcessedEventRegistry(repository);
    }

    @Test
    void markProcessed_eventoNuevo_guardaElEvento() {
        when(repository.existsById(EVENT_ID)).thenReturn(false);

        registry.markProcessed(EVENT_ID);

        ArgumentCaptor<ProcessedKafkaEvent> captor = ArgumentCaptor.forClass(ProcessedKafkaEvent.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getEventId()).isEqualTo(EVENT_ID);
    }

    @Test
    void markProcessed_eventoExistente_noLoGuardaNuevamente() {
        when(repository.existsById(EVENT_ID)).thenReturn(true);

        registry.markProcessed(EVENT_ID);

        verify(repository, never()).save(any(ProcessedKafkaEvent.class));
    }
}
