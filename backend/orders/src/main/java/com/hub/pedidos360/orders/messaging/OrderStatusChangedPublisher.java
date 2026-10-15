package com.hub.pedidos360.orders.messaging;

import com.hub.pedidos360.orders.config.RabbitMQConfig;
import com.hub.pedidos360.orders.model.Order;
import com.hub.pedidos360.orders.model.OrderStatus;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class OrderStatusChangedPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderStatusChangedPublisher.class);

    public static final String EXCHANGE = RabbitMQConfig.EXCHANGE_EVENTS;
    public static final String ROUTING_KEY = RabbitMQConfig.ROUTING_ORDER_STATUS_CHANGED;

    private final EventPublisher eventPublisher;
    private final Validator validator;

    public OrderStatusChangedPublisher(EventPublisher eventPublisher, Validator validator) {
        this.eventPublisher = eventPublisher;
        this.validator = validator;
    }

    public void publish(Order order, OrderStatus previousStatus) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                order.getId(),
                order.getCustomerId(),
                previousStatus.name(),
                order.getStatus().name(),
                order.getUpdatedAt() != null ? order.getUpdatedAt() : LocalDateTime.now()
        );

        Set<ConstraintViolation<OrderStatusChangedEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Evento OrderStatusChanged inválido: " + violations);
        }

        eventPublisher.publish(EXCHANGE, ROUTING_KEY, event);
        log.info("Evento order.status.changed publicado para pedido {} ({} -> {})",
                order.getId(), previousStatus, order.getStatus());
    }
}
