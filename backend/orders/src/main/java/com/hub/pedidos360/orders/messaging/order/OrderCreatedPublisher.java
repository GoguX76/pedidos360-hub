package com.hub.pedidos360.orders.messaging.order;

import com.hub.pedidos360.messaging.MessagingTopology;
import com.hub.pedidos360.orders.messaging.EventPublisher;
import com.hub.pedidos360.orders.model.Order;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OrderCreatedPublisher {

    public static final String EXCHANGE = MessagingTopology.EVENTS_EXCHANGE;
    public static final String ROUTING_KEY = MessagingTopology.ORDER_CREATED_ROUTING_KEY;

    private final EventPublisher eventPublisher;
    private final Validator validator;

    public OrderCreatedPublisher(EventPublisher eventPublisher, Validator validator) {
        this.eventPublisher = eventPublisher;
        this.validator = validator;
    }

    public void publish(Order order) {
        OrderCreatedEvent event = new OrderCreatedEvent(
                order.getId(),
                order.getCustomerId(),
                order.getBranchId(),
                order.getItems().stream()
                        .map(item -> new OrderItemEvent(
                                item.getProductId(),
                                item.getQuantity(),
                                item.getUnitPrice()))
                        .toList(),
                order.getTotalAmount(),
                order.getCreatedAt());

        Set<ConstraintViolation<OrderCreatedEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Evento OrderCreated inválido: " + violations);
        }

        eventPublisher.publish(EXCHANGE, ROUTING_KEY, event);
    }
}
