package com.hub.pedidos360.products.messaging;

import com.hub.pedidos360.products.config.RabbitMQConfig;
import com.hub.pedidos360.products.dto.ProductResponse;
import com.hub.pedidos360.products.service.ProductService;
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
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final ProductService productService;
    private final Validator validator;
    private final StockLowPublisher stockLowPublisher;

    public OrderCreatedConsumer(ProductService productService, Validator validator, StockLowPublisher stockLowPublisher) {
        this.productService = productService;
        this.validator = validator;
        this.stockLowPublisher = stockLowPublisher;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    public void onOrderCreated(OrderCreatedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Set<ConstraintViolation<OrderCreatedEvent>> violations = validator.validate(event);
            if (!violations.isEmpty()) {
                throw new IllegalArgumentException("Evento inválido: " + violations);
            }
            for (OrderItemEvent item : event.getItems()) {
                ProductResponse updated = productService.decrementStock(item.getProductId(), item.getQuantity());
                stockLowPublisher.checkAndPublish(updated);
            }
            channel.basicAck(deliveryTag, false);
            log.info("Pedido {} procesado: stock descontado", event.getOrderId());
        } catch (Exception e) {
            log.error("Error procesando pedido {} (enviando mensaje a DLQ): {}", event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        }
    }
}
