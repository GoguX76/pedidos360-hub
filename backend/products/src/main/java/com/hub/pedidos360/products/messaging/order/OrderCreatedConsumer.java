package com.hub.pedidos360.products.messaging.order;

import com.hub.pedidos360.messaging.MessageRetryHandler;
import com.hub.pedidos360.messaging.MessagingTopology;
import com.hub.pedidos360.messaging.NonRecoverableEventException;
import com.hub.pedidos360.products.dto.ProductResponse;
import com.hub.pedidos360.products.messaging.inventory.StockLowPublisher;
import com.hub.pedidos360.products.service.ProductService;
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
import java.util.Set;

/**
 * Consumidor del dominio de pedidos: descuenta stock por cada item del pedido
 * y delega la alerta de stock bajo al modulo de inventario.
 */
@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final ProductService productService;
    private final Validator validator;
    private final StockLowPublisher stockLowPublisher;
    private final RabbitTemplate rabbitTemplate;
    private final int maxAttempts;

    public OrderCreatedConsumer(ProductService productService, Validator validator,
            StockLowPublisher stockLowPublisher, RabbitTemplate rabbitTemplate,
            @Value("${app.messaging.retry.max-attempts:3}") int maxAttempts) {
        this.productService = productService;
        this.validator = validator;
        this.stockLowPublisher = stockLowPublisher;
        this.rabbitTemplate = rabbitTemplate;
        this.maxAttempts = maxAttempts;
    }

    @RabbitListener(queues = MessagingTopology.ORDER_CREATED_QUEUE)
    public void onOrderCreated(OrderCreatedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Set<ConstraintViolation<OrderCreatedEvent>> violations = validator.validate(event);
            if (!violations.isEmpty()) {
                throw new NonRecoverableEventException("Evento OrderCreated invalido: " + violations);
            }
            for (OrderItemEvent item : event.getItems()) {
                ProductResponse updated = productService.decrementStock(item.getProductId(), item.getQuantity());
                stockLowPublisher.checkAndPublish(updated);
            }
            channel.basicAck(deliveryTag, false);
            log.info("Pedido {} procesado: stock descontado", event.getOrderId());
        } catch (Exception e) {
            MessageRetryHandler.handleFailure(channel, message, asFailure(e), rabbitTemplate, maxAttempts, log,
                    "order.created");
        }
    }

    /**
     * Los errores de negocio (producto inexistente o stock insuficiente) no se
     * arreglan reintentando el mismo mensaje: se marcan como no recuperables
     * para que vayan directo a la DLQ.
     */
    private Exception asFailure(Exception cause) {
        if (cause instanceof com.hub.pedidos360.products.service.ProductNotFoundException
                || cause instanceof com.hub.pedidos360.products.service.InsufficientStockException) {
            return new NonRecoverableEventException(cause.getMessage(), cause);
        }
        return cause;
    }
}
