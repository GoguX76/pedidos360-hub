package com.hub.pedidos360.orders.messaging;

import com.hub.pedidos360.orders.config.RabbitMQConfig;
import com.hub.pedidos360.orders.model.DispatchType;
import com.hub.pedidos360.orders.model.Order;
import com.hub.pedidos360.orders.model.OrderStatus;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderStatusChangedPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    private Validator validator;
    private OrderStatusChangedPublisher publisher;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        publisher = new OrderStatusChangedPublisher(eventPublisher, validator);
    }

    @Test
    @DisplayName("Debe publicar evento order.status.changed cuando la orden cambia de estado válidamente")
    void shouldPublishOrderStatusChangedEvent() {
        Order order = new Order();
        order.setId(10L);
        order.setCustomerId("customer-abc");
        order.setCustomerName("Carlos");
        order.setBranchId(1L);
        order.setStatus(OrderStatus.IN_PREPARATION);
        order.setDispatchType(DispatchType.DELIVERY);
        order.setTotalAmount(new BigDecimal("12000"));
        order.setCreatedAt(LocalDateTime.now().minusHours(1));
        order.setUpdatedAt(LocalDateTime.now());

        publisher.publish(order, OrderStatus.CREATED);

        ArgumentCaptor<OrderStatusChangedEvent> captor = ArgumentCaptor.forClass(OrderStatusChangedEvent.class);
        verify(eventPublisher, times(1)).publish(
                eq(RabbitMQConfig.EXCHANGE_EVENTS),
                eq(RabbitMQConfig.ROUTING_ORDER_STATUS_CHANGED),
                captor.capture()
        );

        OrderStatusChangedEvent published = captor.getValue();
        assertThat(published.getOrderId()).isEqualTo(10L);
        assertThat(published.getCustomerId()).isEqualTo("customer-abc");
        assertThat(published.getPreviousStatus()).isEqualTo("CREATED");
        assertThat(published.getNewStatus()).isEqualTo("IN_PREPARATION");
        assertThat(published.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Debe fallar con IllegalArgumentException si el DTO no cumple validaciones")
    void shouldThrowWhenEventHasInvalidFields() {
        Order order = new Order();
        order.setId(null); // id nulo incumple @NotNull
        order.setCustomerId(""); // customerId vacío incumple @NotBlank
        order.setStatus(OrderStatus.READY);

        assertThrows(IllegalArgumentException.class, () ->
                publisher.publish(order, OrderStatus.IN_PREPARATION)
        );

        verify(eventPublisher, never()).publish(any(), any(), any());
    }
}
