package com.hub.pedidos360.orders.messaging;

import com.hub.pedidos360.orders.config.RabbitMQConfig;
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
import java.util.concurrent.ConcurrentHashMap;

@Component
public class StockLowConsumer {

    private static final Logger log = LoggerFactory.getLogger(StockLowConsumer.class);

    private final Validator validator;
    private final Set<Long> criticalStockProducts = ConcurrentHashMap.newKeySet();

    public StockLowConsumer(Validator validator) {
        this.validator = validator;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_STOCK_LOW)
    public void onStockLow(StockLowEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Set<ConstraintViolation<StockLowEvent>> violations = validator.validate(event);
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException("Evento StockLow inválido: " + violations);
            }

            criticalStockProducts.add(event.getProductId());
            log.warn("ALERTA DE INVENTARIO RECIBIDA en Pedidos: El producto '{}' (ID: {}) tiene stock crítico ({} unidades restantes, umbral: {}).",
                    event.getProductName(), event.getProductId(), event.getCurrentStock(), event.getThreshold());

            channel.basicAck(deliveryTag, false);
            log.info("Mensaje de stock bajo procesado con éxito y confirmado con ACK para producto ID: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Error procesando alerta de stock bajo (enviando mensaje a DLQ): {}", e.getMessage());
            channel.basicReject(deliveryTag, false);
        }
    }

    public boolean isProductInCriticalStock(Long productId) {
        return criticalStockProducts.contains(productId);
    }
}
