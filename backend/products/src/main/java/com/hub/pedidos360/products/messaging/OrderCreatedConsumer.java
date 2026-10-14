package com.hub.pedidos360.products.messaging;

import com.hub.pedidos360.products.config.RabbitMQConfig;
import com.hub.pedidos360.products.service.ProductService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderCreatedConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderCreatedConsumer.class);

    private final ProductService productService;

    public OrderCreatedConsumer(ProductService productService) {
        this.productService = productService;
    }

    @RabbitListener(queues = RabbitMQConfig.QUEUE_ORDER_CREATED)
    public void onOrderCreated(@Valid OrderCreatedEvent event, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            for (OrderItemEvent item : event.getItems()) {
                productService.decrementStock(item.getProductId(), item.getQuantity());
            }
            channel.basicAck(deliveryTag, false);
            log.info("Pedido {} procesado: stock descontado", event.getOrderId());
        } catch (Exception e) {
            log.error("Error procesando pedido {}: {}", event.getOrderId(), e.getMessage());
            channel.basicReject(deliveryTag, false);
        }
    }
}
