package com.hub.pedidos360.products.messaging.inventory;

import com.hub.pedidos360.messaging.MessagingTopology;
import com.hub.pedidos360.products.dto.ProductResponse;
import com.hub.pedidos360.products.messaging.EventPublisher;
import com.hub.pedidos360.products.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLowPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private ProductService productService;

    private StockLowPublisher stockLowPublisher;

    @BeforeEach
    void setUp() {
        stockLowPublisher = new StockLowPublisher(eventPublisher, productService);
    }

    @Test
    @DisplayName("Debe publicar evento cuando el stock es menor o igual al umbral (10)")
    void shouldPublishEventWhenStockIsLow() {
        ProductResponse product = new ProductResponse(1L, "Hamburguesa Clásica", "Rica hamburguesa",
                new BigDecimal("5990"), 5, "Comida", true);

        stockLowPublisher.checkAndPublish(product);

        ArgumentCaptor<StockLowEvent> eventCaptor = ArgumentCaptor.forClass(StockLowEvent.class);
        verify(eventPublisher, times(1)).publish(
                eq(MessagingTopology.INVENTORY_EXCHANGE),
                eq(MessagingTopology.STOCK_LOW_ROUTING_KEY),
                eventCaptor.capture()
        );

        StockLowEvent published = eventCaptor.getValue();
        assertThat(published.getProductId()).isEqualTo(1L);
        assertThat(published.getProductName()).isEqualTo("Hamburguesa Clásica");
        assertThat(published.getCurrentStock()).isEqualTo(5);
        assertThat(published.getThreshold()).isEqualTo(10);
        assertThat(published.getAlertAt()).isNotNull();
    }

    @Test
    @DisplayName("NO debe publicar evento cuando el stock es mayor al umbral")
    void shouldNotPublishEventWhenStockIsSufficient() {
        ProductResponse product = new ProductResponse(2L, "Papas Fritas", "Papas crujientes",
                new BigDecimal("2990"), 25, "Acompañamiento", true);

        stockLowPublisher.checkAndPublish(product);

        verify(eventPublisher, never()).publish(any(), any(), any());
    }

    @Test
    @DisplayName("Debe resolver el producto por ID y publicar si el stock es bajo")
    void shouldFetchByIdAndPublish() {
        ProductResponse product = new ProductResponse(3L, "Bebida Cola", "Refresco 500ml",
                new BigDecimal("1500"), 8, "Bebidas", true);
        when(productService.getById(3L)).thenReturn(product);

        stockLowPublisher.checkAndPublish(3L);

        verify(productService, times(1)).getById(3L);
        verify(eventPublisher, times(1)).publish(
                eq(MessagingTopology.INVENTORY_EXCHANGE),
                eq(MessagingTopology.STOCK_LOW_ROUTING_KEY),
                any(StockLowEvent.class)
        );
    }
}
