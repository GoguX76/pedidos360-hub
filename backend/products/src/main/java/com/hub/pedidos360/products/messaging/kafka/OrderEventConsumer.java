package com.hub.pedidos360.products.messaging.kafka;

import com.hub.pedidos360.messaging.kafka.KafkaTopics;
import com.hub.pedidos360.messaging.kafka.OrderEvent;
import com.hub.pedidos360.products.notification.OrderNotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Consume los eventos de pedido publicados por orders y genera la
 * notificacion correspondiente en products.
 */
@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final Validator validator;
    private final OrderNotificationService notificationService;
    private final ProcessedEventRegistry processedEvents;

    public OrderEventConsumer(Validator validator,
            OrderNotificationService notificationService,
            ProcessedEventRegistry processedEvents) {
        this.validator = validator;
        this.notificationService = notificationService;
        this.processedEvents = processedEvents;
    }

    @KafkaListener(
            topics = KafkaTopics.ORDER_EVENTS,
            groupId = "${app.kafka.products-consumer-group:products-order-events}",
            containerFactory = "orderEventListenerContainerFactory")
    public void onOrderEvent(OrderEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "No se pudo leer el evento de pedido (JSON invalido o evento nulo)");
        }

        Set<ConstraintViolation<OrderEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Evento OrderEvent invalido: " + violations);
        }

        if (processedEvents.isProcessed(event.eventId())) {
            log.info("El evento de pedido {} ya fue procesado; se ignora el duplicado", event.eventId());
            return;
        }

        notificationService.notifyOrderEvent(event);
        processedEvents.markProcessed(event.eventId());
    }
}
