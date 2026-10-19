package com.hub.pedidos360.orders.messaging.inventory;

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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Consumidor de alertas de stock bajo. Agrupa la logica del dominio de
 * inventario: los errores recuperables se reintentan y, agotados los intentos o
 * ante un evento invalido, el mensaje termina en la DLQ.
 */
@Component
public class StockLowConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockLowConsumer.class);

    /** Cota del registro en memoria para evitar crecimiento ilimitado. */
    private static final int MAX_TRACKED_PRODUCTS = 10_000;

    private final Validator validator;
    private final RabbitTemplate rabbitTemplate;
    private final int maxAttempts;

    /**
     * Registro en memoria (LRU acotado) de productos ya marcados con stock
     * critico. Es volatil por diseno: se usa solo para evitar repetir la misma
     * alerta repetidamente mientras el servicio esta arriba.
     */
    private final Set<Long> criticalStockProducts = Collections.newSetFromMap(
            Collections.synchronizedMap(new LinkedHashMap<>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, Boolean> eldest) {
                    return size() > MAX_TRACKED_PRODUCTS;
                }
            }));

    public StockLowConsumer(Validator validator, RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.retry.max-attempts:3}") int maxAttempts) {
        this.validator = validator;
        this.rabbitTemplate = rabbitTemplate;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = MessagingTopology.STOCK_LOW_QUEUE)
    public void onStockLow(StockLowEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Set<ConstraintViolation<StockLowEvent>> violations = validator.validate(event);
            if (!violations.isEmpty()) {
                throw new NonRecoverableEventException("Evento StockLow invalido: " + violations);
            }

            criticalStockProducts.add(event.getProductId());
            log.warn("ALERTA DE INVENTARIO RECIBIDA en Pedidos: El producto '{}' (ID: {}) tiene stock critico ({} unidades restantes, umbral: {}).",
                    event.getProductName(), event.getProductId(), event.getCurrentStock(), event.getThreshold());

            channel.basicAck(deliveryTag, false);
            log.info("Mensaje de stock bajo procesado con exito y confirmado con ACK para producto ID: {}", event.getProductId());
        } catch (Exception e) {
            MessageRetryHandler.handleFailure(channel, message, e, rabbitTemplate, maxAttempts, log,
                    "inventory.stock-low");
        }
    }

    public boolean isProductInCriticalStock(Long productId) {
        return criticalStockProducts.contains(productId);
    }
}
