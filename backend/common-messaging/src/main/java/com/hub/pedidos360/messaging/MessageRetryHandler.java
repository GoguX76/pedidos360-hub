package com.hub.pedidos360.messaging;

import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;

/**
 * Centraliza la politica de manejo de errores de todos los consumidores:
 *
 * <ul>
 *   <li>Errores <b>no recuperables</b> ({@link NonRecoverableEventException}) se
 *       rechazan con {@code basicReject(requeue=false)} y van directo a la DLQ.</li>
 *   <li>Errores <b>recuperables</b> se reintentan hasta {@code maxAttempts},
 *       reencolando el mensaje con un contador en el header
 *       {@value #RETRY_COUNT_HEADER} para no caer en un bucle infinito.</li>
 *   <li>Agotados los reintentos, el mensaje se rechaza y termina en la DLQ.</li>
 * </ul>
 */
public final class MessageRetryHandler {

    /** Header donde se lleva la cuenta de reintentos de un mensaje. */
    public static final String RETRY_COUNT_HEADER = "x-retry-count";

    private MessageRetryHandler() {
    }

    /** Cantidad de reintentos ya consumidos por este mensaje (0 si es la primera entrega). */
    public static int retryCount(Message message) {
        Object value = message.getMessageProperties().getHeader(RETRY_COUNT_HEADER);
        return value instanceof Number number ? number.intValue() : 0;
    }

    /**
     * Decide la suerte del mensaje que fallo y confirma/rechaza explicitamente el
     * delivery tag.
     *
     * @param maxAttempts cantidad maxima de intentos (incluye el primero)
     */
    public static void handleFailure(Channel channel, Message message, Exception cause,
            RabbitTemplate rabbitTemplate, int maxAttempts, Logger log, String context) throws IOException {

        long deliveryTag = message.getMessageProperties().getDeliveryTag();

        if (cause instanceof NonRecoverableEventException) {
            log.error("[{}] Error no recuperable: {}. Enviando a DLQ.", context, cause.getMessage());
            channel.basicReject(deliveryTag, false);
            return;
        }

        int attempt = retryCount(message) + 1;
        if (attempt < maxAttempts) {
            log.warn("[{}] Error recuperable, reintento {}/{}: {}. Reencolando.",
                    context, attempt, maxAttempts, cause.getMessage());
            republish(message, attempt, rabbitTemplate);
            channel.basicAck(deliveryTag, false);
        } else {
            log.error("[{}] Reintentos agotados ({} intentos): {}. Enviando a DLQ.",
                    context, maxAttempts, cause.getMessage());
            channel.basicReject(deliveryTag, false);
        }
    }

    private static void republish(Message message, int attempt, RabbitTemplate rabbitTemplate) {
        MessageProperties source = message.getMessageProperties();
        MessageProperties target = new MessageProperties();
        target.setContentType(source.getContentType());
        target.setContentEncoding(source.getContentEncoding());
        target.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
        target.setCorrelationId(source.getCorrelationId());
        target.setMessageId(source.getMessageId());
        target.setHeader(RETRY_COUNT_HEADER, attempt);

        rabbitTemplate.send(source.getReceivedExchange(), source.getReceivedRoutingKey(),
                new Message(message.getBody(), target));
    }
}
