package com.hub.pedidos360.orders.messaging.inventory;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockLowConsumerTest {

    private static final int MAX_ATTEMPTS = 3;

    @Mock
    private Validator validator;

    @Mock
    private Channel channel;

    @Mock
    private Message message;

    @Mock
    private RabbitTemplate rabbitTemplate;

    private StockLowConsumer consumer;

    @BeforeEach
    void setUp() {
        MessageProperties props = new MessageProperties();
        props.setDeliveryTag(77L);
        props.setReceivedExchange("pedidos360.inventory");
        props.setReceivedRoutingKey("stock.low");
        when(message.getMessageProperties()).thenReturn(props);
        consumer = new StockLowConsumer(validator, rabbitTemplate, MAX_ATTEMPTS);
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
        verifyNoInteractions(rabbitTemplate);
    }

    @SuppressWarnings("unchecked")
    @Test
    @DisplayName("Debe rechazar con basicReject (DLQ directa) si el evento es inválido")
    void shouldRejectToDlqWhenEventIsInvalid() throws IOException {
        StockLowEvent event = new StockLowEvent(null, "", -1, 10, LocalDateTime.now());

        ConstraintViolation<StockLowEvent> violation = mock(ConstraintViolation.class);
        when(validator.validate(event)).thenReturn(Set.of(violation));

        consumer.onStockLow(event, message, channel);

        verify(channel, times(1)).basicReject(77L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }

    @Test
    @DisplayName("Debe reencolar (reintento) y confirmar cuando el error es recuperable")
    void shouldRepublishAndAckWhenErrorIsRecoverable() throws IOException {
        StockLowEvent event = new StockLowEvent(9L, "Bebida", 2, 10, LocalDateTime.now());

        when(validator.validate(event)).thenThrow(new IllegalStateException("Base de datos no disponible"));
        when(message.getBody()).thenReturn("{}".getBytes(StandardCharsets.UTF_8));

        consumer.onStockLow(event, message, channel);

        verify(rabbitTemplate, times(1)).send(eq("pedidos360.inventory"), eq("stock.low"), any(Message.class));
        verify(channel, times(1)).basicAck(77L, false);
        verify(channel, never()).basicReject(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("Debe enviar a DLQ con basicReject cuando se agotan los reintentos")
    void shouldRejectToDlqWhenRetriesAreExhausted() throws IOException {
        StockLowEvent event = new StockLowEvent(9L, "Bebida", 2, 10, LocalDateTime.now());
        message.getMessageProperties().setHeader(MessageRetryHandler.RETRY_COUNT_HEADER, MAX_ATTEMPTS - 1);

        when(validator.validate(event)).thenThrow(new IllegalStateException("Base de datos no disponible"));

        consumer.onStockLow(event, message, channel);

        verify(channel, times(1)).basicReject(77L, false);
        verify(channel, never()).basicAck(anyLong(), anyBoolean());
        verify(rabbitTemplate, never()).send(anyString(), anyString(), any(Message.class));
    }
}
