package com.hub.pedidos360.kafka_admin.exception;

public class TopicNotFoundException extends KafkaAdminException {
    public TopicNotFoundException(String message) {
        super(message);
    }
}
