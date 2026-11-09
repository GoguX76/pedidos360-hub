package com.hub.pedidos360.kafka_admin.exception;

public class KafkaAdminException extends RuntimeException {
    public KafkaAdminException(String message) {
        super(message);
    }

    public KafkaAdminException(String message, Throwable cause) {
        super(message, cause);
    }
}
