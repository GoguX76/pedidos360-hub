package com.hub.pedidos360.products.messaging.order;

import com.hub.pedidos360.messaging.MessageRetryHandler;
import com.hub.pedidos360.messaging.MessagingTopology;
import com.hub.pedidos360.messaging.NonRecoverableEventException;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

/**
 * Consumidor del dominio de pedidos: descuenta stock por cada item del pedido
 * y delega la alerta de stock bajo al modulo de inventario.
 */
@Component
public class OrderStatusChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedConsumer.class);

    private final Validator validator;
    private final RabbitTemplate rabbitTemplate;
    private final int maxAttempts;

    public OrderStatusChangedConsumer(Validator validator, RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.retry.max-attempts:3}") int maxAttempts) {
        this.validator = validator;
        this.rabbitTemplate = rabbitTemplate;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = MessagingTopology.ORDER_STATUS_CHANGED_QUEUE)
    public void onOrderStatusChanged(OrderStatusChangedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Set<ConstraintViolation<OrderStatusChangedEvent>> violations = validator.validate(event);
            if (!violations.isEmpty()) {
                throw new NonRecoverableEventException("Evento OrderStatusChanged invalido: " + violations);
            }

            log.info("Notificacion de cambio de estado recibida en Productos para pedido {}: {} -> {}",
                    event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            MessageRetryHandler.handleFailure(channel, message, e, rabbitTemplate, maxAttempts, log,
                    "order.status-changed");
        }
    }
}
