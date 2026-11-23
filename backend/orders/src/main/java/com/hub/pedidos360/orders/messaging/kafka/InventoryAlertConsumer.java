package com.hub.pedidos360.orders.messaging.kafka;

import com.hub.pedidos360.messaging.kafka.InventoryAlertEvent;
import com.hub.pedidos360.messaging.kafka.KafkaTopics;
import com.hub.pedidos360.orders.notification.InventoryNotificationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Consume las alertas de stock bajo publicadas por products y genera la
 * notificacion correspondiente en orders.
 */
@Component
public class InventoryAlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(InventoryAlertConsumer.class);

    private final Validator validator;
    private final InventoryNotificationService notificationService;
    private final ProcessedEventRegistry processedEvents;

    public InventoryAlertConsumer(Validator validator,
            InventoryNotificationService notificationService,
            ProcessedEventRegistry processedEvents) {
        this.validator = validator;
        this.notificationService = notificationService;
        this.processedEvents = processedEvents;
    }

    @KafkaListener(
            topics = KafkaTopics.INVENTORY_ALERTS,
            groupId = "${app.kafka.orders-consumer-group:orders-notifications}",
            containerFactory = "inventoryAlertListenerContainerFactory")
    public void onInventoryAlert(InventoryAlertEvent event) {
        if (event == null) {
            throw new IllegalArgumentException(
                    "No se pudo leer el evento de inventario (JSON invalido o evento nulo)");
        }

        Set<ConstraintViolation<InventoryAlertEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Evento InventoryAlert invalido: " + violations);
        }

        if (processedEvents.isProcessed(event.eventId())) {
            log.info("El evento de inventario {} ya fue procesado; se ignora el duplicado", event.eventId());
            return;
        }

        notificationService.notifyLowStock(event);
        processedEvents.markProcessed(event.eventId());
    }
}
