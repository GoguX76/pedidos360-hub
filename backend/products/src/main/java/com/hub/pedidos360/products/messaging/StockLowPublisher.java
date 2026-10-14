package com.hub.pedidos360.products.messaging;

import com.hub.pedidos360.products.config.RabbitMQConfig;
import com.hub.pedidos360.products.service.ProductService;
import org.springframework.stereotype.Component;

@Component
public class StockLowPublisher {
    private final EventPublisher eventPublisher;
    private final ProductService productService;

    public StockLowPublisher(EventPublisher eventPublisher, ProductService productService) {
        this.eventPublisher = eventPublisher;
        this.productService = productService;
    }

    public void checkAndPublish(Long productId) {
        var product = productService.getById(productId);
        if (product.getStock() <= 10) {
            eventPublisher.publish(
                RabbitMQConfig.EXCHANGE_INVENTORY,
                RabbitMQConfig.ROUTING_STOCK_LOW,
                "Stock bajo para producto " + product.getId() + ": " + product.getStock());
        }
    }
}
