package com.hub.pedidos360.orders.messaging;

import com.hub.pedidos360.orders.model.Order;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class OrderCreatedPublisher {

    public static final String EXCHANGE = "pedidos360.events";
    public static final String ROUTING_KEY = "order.created";

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