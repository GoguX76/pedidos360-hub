package com.hub.pedidos360.kafka_admin.exception;

public class InvalidTopicRequestException extends KafkaAdminException {
    public InvalidTopicRequestException(String message) {
        super(message);
    }
}
