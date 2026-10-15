package com.hub.pedidos360.orders.messaging;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLowConsumerTest {

    @Mock
    private Validator validator;

    @Mock
    private Channel channel;

    @Mock
    private Message message;

    private StockLowConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new StockLowConsumer(validator);
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(77L);
        when(message.getMessageProperties()).thenReturn(props);
    }

    @Test
    @DisplayName("Debe procesar alerta de stock bajo, registrar producto crítico y emitir basicAck")
    void shouldProcessStockLowAlertSuccessfully() throws IOException {
        StockLowEvent event = new StockLowEvent(
                5L,
                "Papas Supremas",
                3,
                10,
                LocalDateTime.now()
        );

        when(validator.validate(event)).thenReturn(Collections.emptySet());

        consumer.onStockLow(event, message, channel);

        assertThat(consumer.isProductInCriticalStock(5L)).isTrue();
        verify(channel, times(1)).basicAck(77L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Debe rechazar con basicReject (DLQ) si la validación falla o hay error")
    void shouldRejectToDlqOnError() throws IOException {
        StockLowEvent event = new StockLowEvent(
                null,
                "",
                -1,
                10,
                LocalDateTime.now()
        );

        when(validator.validate(event)).thenThrow(new IllegalArgumentException("Evento inválido"));

        consumer.onStockLow(event, message, channel);

        verify(channel, times(1)).basicReject(77L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
    }
}
