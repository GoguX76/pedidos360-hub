package com.hub.pedidos360.orders.messaging.kafka;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedKafkaEventRepository
        extends JpaRepository<ProcessedKafkaEvent, String> {
}