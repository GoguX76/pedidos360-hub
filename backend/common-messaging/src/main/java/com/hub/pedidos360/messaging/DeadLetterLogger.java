package com.hub.pedidos360.messaging;

import org.slf4j.Logger;
import org.springframework.amqp.core.Message;

import java.nio.charset.StandardCharsets;

/**
 * Registra en los logs los mensajes que terminaron en una cola muerta,
 * incluyendo el header {@code x-death} que RabbitMQ agrega al dead-letter.
 */
public final class DeadLetterLogger {

    private DeadLetterLogger() {
    }

    public static void log(Logger log, String queue, Message message) {
        Object xDeath = message.getMessageProperties().getHeader("x-death");
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        log.error("Mensaje muerto en '{}'. Motivo x-death: {}. Payload: {}", queue, xDeath, body);
    }
}
