package com.hub.pedidos360.products.messaging;

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
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStatusChangedConsumerTest {

    @Mock
    private Validator validator;

    @Mock
    private Channel channel;

    @Mock
    private Message message;

    private OrderStatusChangedConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderStatusChangedConsumer(validator);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(88L);
        when(message.getMessageProperties()).thenReturn(props);
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
    @DisplayName("Debe enviar a DLQ con basicReject si ocurre un error de validación")
    void shouldRejectToDlqOnError() throws IOException {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                null,
                "",
                null,
                null,
                null
        );

        when(validator.validate(event)).thenThrow(new IllegalArgumentException("Evento inválido"));

        consumer.onOrderStatusChanged(event, message, channel);

        verify(channel, times(1)).basicReject(88L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
