package com.hub.pedidos360.products.messaging.kafka;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedKafkaEventRepository
        extends JpaRepository<ProcessedKafkaEvent, String> {
}