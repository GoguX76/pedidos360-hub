package com.hub.pedidos360.products.messaging;

import com.hub.pedidos360.products.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Component
public class OrderStatusChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedConsumer.class);

    private final Validator validator;

    public OrderStatusChangedConsumer(Validator validator) {
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_STATUS_CHANGED)
    public void onOrderStatusChanged(OrderStatusChangedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Set<ConstraintViolation<OrderStatusChangedEvent>> violations = validator.validate(event);
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException("Evento OrderStatusChanged inválido: " + violations);
            }

            log.info("Notificación de cambio de estado recibida en Productos para pedido {}: {} -> {}",
                    event.getOrderId(), event.getPreviousStatus(), event.getNewStatus());

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Error procesando cambio de estado para pedido {} (enviando mensaje a DLQ): {}",
                    event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        }
    }
}
