package com.hub.pedidos360.kafka_admin.exception;

public class TopicAlreadyExistsException extends KafkaAdminException {
    public TopicAlreadyExistsException(String message) {
        super(message);
    }
}
