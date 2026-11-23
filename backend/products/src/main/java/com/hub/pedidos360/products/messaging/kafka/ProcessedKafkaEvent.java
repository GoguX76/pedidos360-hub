package com.hub.pedidos360.products.messaging.kafka;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "processed_kafka_events")
public class ProcessedKafkaEvent {

    /**
     * Identifica el evento kafka. Evita duplicidad
     */
    @Id
    @Column(name = "event_id", nullable = false, updatable = false, length = 255)
    private String eventId;

    /**
     * Registra cuando se guardo el evento kafka
     */
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedKafkaEvent() {
    }

    public ProcessedKafkaEvent(String eventId) {
        this.eventId = eventId;
        this.processedAt = Instant.now();
    }

    public String getEventId() {
        return eventId;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}