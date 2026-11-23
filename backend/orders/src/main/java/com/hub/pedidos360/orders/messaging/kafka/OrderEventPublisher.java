package com.hub.pedidos360.orders.messaging.kafka;

import com.hub.pedidos360.messaging.kafka.KafkaTopics;
import com.hub.pedidos360.messaging.kafka.OrderEvent;
import com.hub.pedidos360.orders.model.Order;
import com.hub.pedidos360.orders.model.OrderItem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Publica eventos de pedido en el topico {@code pedidos.eventos}.
 *
 * <p>La clave del mensaje es el id del pedido, de modo que todos los eventos
 * de un mismo pedido caen en la misma particion y conservan el orden.</p>
 */
@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Validator validator;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, Validator validator) {
        this.kafkaTemplate = kafkaTemplate;
        this.validator = validator;
    }

    public void publishCreated(Order order) {
        publish(order, OrderEvent.TYPE_CREATED, order.getCreatedAt(), order.getStatus().name());
    }

    public void publishStatusChanged(Order order) {
        LocalDateTime occurredAt = order.getUpdatedAt() != null
                ? order.getUpdatedAt()
                : LocalDateTime.now();
        publish(order, OrderEvent.TYPE_STATUS_CHANGED, occurredAt, order.getStatus().name());
    }

    private void publish(Order order, String eventType, LocalDateTime occurredAt, String status) {
        OrderEvent event = toEvent(order, eventType, occurredAt, status);
        validate(event);

        String key = String.valueOf(order.getId());
        kafkaTemplate.send(KafkaTopics.ORDER_EVENTS, key, event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("No se pudo publicar el evento {} del pedido {} en Kafka: {}",
                                eventType, order.getId(), error.getMessage(), error);
                        return;
                    }
                    log.info("Evento {} del pedido {} publicado en Kafka (topico={}, particion={}, offset={})",
                            eventType,
                            order.getId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }

    private OrderEvent toEvent(Order order, String eventType, LocalDateTime occurredAt, String status) {
        List<OrderEvent.Item> items = order.getItems().stream()
                .map(this::toItem)
                .toList();

        return new OrderEvent(
                UUID.randomUUID().toString(),
                eventType,
                order.getId(),
                order.getCustomerId(),
                order.getCustomerName(),
                order.getBranchId(),
                order.getDispatchType().name(),
                status,
                order.getTotalAmount(),
                items,
                occurredAt.toString());
    }

    private OrderEvent.Item toItem(OrderItem item) {
        return new OrderEvent.Item(
                item.getProductId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice());
    }

    private void validate(OrderEvent event) {
        Set<ConstraintViolation<OrderEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Evento OrderEvent invalido: " + violations);
        }
    }
}
