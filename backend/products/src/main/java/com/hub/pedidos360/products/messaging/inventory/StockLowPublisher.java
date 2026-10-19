package com.hub.pedidos360.products.messaging.inventory;

import com.hub.pedidos360.messaging.MessagingTopology;
import com.hub.pedidos360.products.dto.ProductResponse;
import com.hub.pedidos360.products.messaging.EventPublisher;
import com.hub.pedidos360.products.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Publica la alerta de stock bajo del dominio de inventario.
 */
@Component
public class StockLowPublisher {

    private static final Logger log = LoggerFactory.getLogger(StockLowPublisher.class);
    public static final int STOCK_THRESHOLD = 10;

    private final EventPublisher eventPublisher;
    private final ProductService productService;

    public StockLowPublisher(EventPublisher eventPublisher, ProductService productService) {
        this.eventPublisher = eventPublisher;
        this.productService = productService;
    }

    public void checkAndPublish(Long productId) {
        ProductResponse product = productService.getById(productId);
        checkAndPublish(product);
    }

    public void checkAndPublish(ProductResponse product) {
        if (product.getStock() <= STOCK_THRESHOLD) {
            StockLowEvent event = new StockLowEvent(
                    product.getId(),
                    product.getName(),
                    product.getStock(),
                    STOCK_THRESHOLD,
                    LocalDateTime.now()
            );
            eventPublisher.publish(
                    MessagingTopology.INVENTORY_EXCHANGE,
                    MessagingTopology.STOCK_LOW_ROUTING_KEY,
                    event
            );
            log.info("Alerta de stock bajo emitida para producto '{}' (ID: {}). Stock restante: {}",
                    product.getName(), product.getId(), product.getStock());
        }
    }
}
