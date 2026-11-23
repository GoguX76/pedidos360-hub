package com.hub.pedidos360.products.messaging.kafka;

import org.springframework.stereotype.Component;

/**
 * Registro persistente de eventos Kafka ya procesados.
 *
 * Consulta la base de datos del servicio para reconocer reentregas,
 * incluso después de reiniciar la aplicación.
 */

@Component
public class ProcessedEventRegistry {

    private final ProcessedKafkaEventRepository repository;

    public ProcessedEventRegistry(ProcessedKafkaEventRepository repository) {
        this.repository = repository;
    }

    public boolean isProcessed(String eventId) {
        return repository.existsById(eventId);
    }

    public void markProcessed(String eventId) {
        if (!isProcessed(eventId)) {
            repository.save(new ProcessedKafkaEvent(eventId));
        }
    }
}
