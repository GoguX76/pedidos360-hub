package com.hub.pedidos360.products.messaging;

import com.hub.pedidos360.products.dto.ProductResponse;
import com.hub.pedidos360.products.service.ProductService;
import com.rabbitmq.client.Channel;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    @Mock
    private ProductService productService;

    @Mock
    private Validator validator;

    @Mock
    private StockLowPublisher stockLowPublisher;

    @Mock
    private Channel channel;

    @Mock
    private Message message;

    private OrderCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderCreatedConsumer(productService, validator, stockLowPublisher);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(42L);
        when(message.getMessageProperties()).thenReturn(props);
    }

    @Test
    @DisplayName("Debe descontar stock, verificar stock bajo y hacer basicAck cuando el evento es válido")
    void shouldProcessOrderSuccessfully() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent(
                100L,
                "user-123",
                1L,
                List.of(new OrderItemEvent(1L, 2, new BigDecimal("5990"))),
                new BigDecimal("11980"),
                LocalDateTime.now()
        );

        when(validator.validate(event)).thenReturn(Collections.emptySet());

        ProductResponse updatedProduct = new ProductResponse(1L, "Hamburguesa", "Desc",
                new BigDecimal("5990"), 3, "Comida", true);
        when(productService.decrementStock(1L, 2)).thenReturn(updatedProduct);

        consumer.onOrderCreated(event, message, channel);

        verify(productService, times(1)).decrementStock(1L, 2);
        verify(stockLowPublisher, times(1)).checkAndPublish(updatedProduct);
        verify(channel, times(1)).basicAck(42L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Debe hacer basicReject (DLQ) cuando ocurre un error al procesar")
    void shouldRejectMessageToDlqOnError() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent(
                101L,
                "user-123",
                1L,
                List.of(new OrderItemEvent(999L, 2, new BigDecimal("1000"))),
                new BigDecimal("2000"),
                LocalDateTime.now()
        );

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(productService.decrementStock(999L, 2)).thenThrow(new RuntimeException("Producto no encontrado"));

        consumer.onOrderCreated(event, message, channel);

        verify(channel, times(1)).basicReject(42L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
