package com.hub.pedidos360.products.messaging.order;

import com.hub.pedidos360.messaging.MessageRetryHandler;
import com.rabbitmq.client.Channel;
import jakarta.validation.ConstraintViolation;
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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStatusChangedConsumerTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private Validator validator;

    @Mock
    private Channel channel;

    @Mock
    private Message message;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private OrderStatusChangedConsumer consumer;

    @BeforeEach
    void setUp() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(88L);
        props.setReceivedExchange("pedidos360.events");
        props.setReceivedRoutingKey("order.status.changed");
        when(message.getMessageProperties()).thenReturn(props);
        consumer = new OrderStatusChangedConsumer(validator, rabbitTemplate, MAX_ATTEMPTS);
    }

    @Test
    @DisplayName("Debe procesar cambio de estado y realizar basicAck")
    void shouldProcessStatusChangeSuccessfully() throws IOException {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                20L,
                "cust-99",
                "CREATED",
                "IN_PREPARATION",
                LocalDateTime.now()
        );

        when(validator.validate(event)).thenReturn(Collections.emptySet());

        consumer.onOrderStatusChanged(event, message, channel);

        verify(channel, times(1)).basicAck(88L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Debe enviar a DLQ sin reintentar si el evento no cumple validaciones")
    void shouldRejectToDlqWhenEventIsInvalid() throws IOException {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                null,
                "",
                null,
                null,
                null
        );

        ConstraintViolation<OrderStatusChangedEvent> violation = mock(ConstraintViolation.class);
        when(validator.validate(event)).thenReturn(Set.of(violation));

        consumer.onOrderStatusChanged(event, message, channel);

        verify(channel, times(1)).basicReject(88L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Debe reencolar y confirmar ante un error recuperable")
    void shouldRepublishAndAckWhenErrorIsRecoverable() throws IOException {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                21L,
                "cust-99",
                "CREATED",
                "IN_PREPARATION",
                LocalDateTime.now()
        );

        when(validator.validate(event)).thenThrow(new IllegalStateException("Base de datos no disponible"));
        when(message.getBody()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));

        consumer.onOrderStatusChanged(event, message, channel);

        verify(rabbitTemplate, times(1)).send(eq("pedidos360.events"), eq("order.status.changed"), any(Message.class));
        verify(channel, times(1)).basicAck(88L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Debe enviar a DLQ cuando se agotan los reintentos")
    void shouldRejectToDlqWhenRetriesAreExhausted() throws IOException {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                22L,
                "cust-99",
                "CREATED",
                "IN_PREPARATION",
                LocalDateTime.now()
        );
        message.getMessageProperties().setHeader(MessageRetryHandler.RETRY_COUNT_HEADER, MAX_ATTEMPTS - 1);

        when(validator.validate(event)).thenThrow(new IllegalStateException("Base de datos no disponible"));

        consumer.onOrderStatusChanged(event, message, channel);

        verify(channel, times(1)).basicReject(88L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }
}
