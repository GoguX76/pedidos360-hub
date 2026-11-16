package com.hub.pedidos360.products.messaging.kafka;

import com.hub.pedidos360.messaging.kafka.InventoryAlertEvent;
import com.hub.pedidos360.messaging.kafka.KafkaTopics;
import com.hub.pedidos360.products.dto.ProductResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * Publica alertas de stock bajo en el topico {@code inventario.alertas}.
 *
 * <p>Se usa desde {@code StockLowPublisher} despues de publicar en RabbitMQ,
 * de modo que ambos flujos conviven sin reemplazarse.</p>
 */
@Component
public class KafkaInventoryAlertPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaInventoryAlertPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Validator validator;

    public KafkaInventoryAlertPublisher(KafkaTemplate<String, Object> kafkaTemplate, Validator validator) {
        this.kafkaTemplate = kafkaTemplate;
        this.validator = validator;
    }

    public void publish(ProductResponse product, int threshold) {
        InventoryAlertEvent event = new InventoryAlertEvent(
                UUID.randomUUID().toString(),
                product.getId(),
                product.getName(),
                product.getStock(),
                threshold,
                LocalDateTime.now().toString());

        validate(event);

        String key = String.valueOf(product.getId());
        kafkaTemplate.send(KafkaTopics.INVENTORY_ALERTS, key, event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("No se pudo publicar la alerta de stock bajo del producto {} en Kafka: {}",
                                product.getId(), error.getMessage(), error);
                        return;
                    }
                    log.info("Alerta de stock bajo del producto {} publicada en Kafka "
                                    + "(topico={}, particion={}, offset={})",
                            product.getId(),
                            result.getRecordMetadata().topic(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                });
    }

    private void validate(InventoryAlertEvent event) {
        Set<ConstraintViolation<InventoryAlertEvent>> violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Evento InventoryAlert invalido: " + violations);
        }
    }
}
