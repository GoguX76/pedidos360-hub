package com.hub.pedidos360.products.messaging.order;

import com.hub.pedidos360.messaging.MessageRetryHandler;
import com.hub.pedidos360.products.dto.ProductResponse;
import com.hub.pedidos360.products.messaging.inventory.StockLowPublisher;
import com.hub.pedidos360.products.service.ProductNotFoundException;
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
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreatedConsumerTest {

    private static final int MAX_ATTEMPTS = 3;

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

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderCreatedConsumer consumer;

    @BeforeEach
    void setUp() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(42L);
        props.setReceivedExchange("pedidos360.events");
        props.setReceivedRoutingKey("order.created");
        when(message.getMessageProperties()).thenReturn(props);
        consumer = new OrderCreatedConsumer(productService, validator, stockLowPublisher, rabbitTemplate,
                MAX_ATTEMPTS);
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
    @DisplayName("Debe enviar a DLQ sin reintentar cuando el producto no existe (error no recuperable)")
    void shouldRejectToDlqWhenProductNotFound() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent(
                101L,
                "user-123",
                1L,
                List.of(new OrderItemEvent(999L, 2, new BigDecimal("1000"))),
                new BigDecimal("2000"),
                LocalDateTime.now()
        );

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(productService.decrementStock(999L, 2)).thenThrow(new ProductNotFoundException(999L));

        consumer.onOrderCreated(event, message, channel);

        verify(channel, times(1)).basicReject(42L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Debe reencolar y confirmar cuando el error es recuperable")
    void shouldRepublishAndAckWhenErrorIsRecoverable() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent(
                102L,
                "user-123",
                1L,
                List.of(new OrderItemEvent(1L, 2, new BigDecimal("1000"))),
                new BigDecimal("2000"),
                LocalDateTime.now()
        );

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(productService.decrementStock(1L, 2)).thenThrow(new IllegalStateException("Base de datos no disponible"));
        when(message.getBody()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));

        consumer.onOrderCreated(event, message, channel);

        verify(rabbitTemplate, times(1)).send(eq("pedidos360.events"), eq("order.created"), any(Message.class));
        verify(channel, times(1)).basicAck(42L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Debe enviar a DLQ cuando se agotan los reintentos")
    void shouldRejectToDlqWhenRetriesAreExhausted() throws IOException {
        OrderCreatedEvent event = new OrderCreatedEvent(
                103L,
                "user-123",
                1L,
                List.of(new OrderItemEvent(1L, 2, new BigDecimal("1000"))),
                new BigDecimal("2000"),
                LocalDateTime.now()
        );
        message.getMessageProperties().setHeader(MessageRetryHandler.RETRY_COUNT_HEADER, MAX_ATTEMPTS - 1);

        when(validator.validate(event)).thenReturn(Collections.emptySet());
        when(productService.decrementStock(1L, 2)).thenThrow(new IllegalStateException("Base de datos no disponible"));

        consumer.onOrderCreated(event, message, channel);

        verify(channel, times(1)).basicReject(42L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }
}
