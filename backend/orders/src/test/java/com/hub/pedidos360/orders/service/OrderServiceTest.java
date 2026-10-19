package com.hub.pedidos360.orders.service;

import com.hub.pedidos360.orders.dto.OrderResponse;
import com.hub.pedidos360.orders.messaging.order.OrderCreatedPublisher;
import com.hub.pedidos360.orders.messaging.order.OrderStatusChangedPublisher;
import com.hub.pedidos360.orders.model.DispatchType;
import com.hub.pedidos360.orders.model.Order;
import com.hub.pedidos360.orders.model.OrderStatus;
import com.hub.pedidos360.orders.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCreatedPublisher orderCreatedPublisher;

    @Mock
    private OrderStatusChangedPublisher orderStatusChangedPublisher;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, orderCreatedPublisher, orderStatusChangedPublisher);
    }

    @Test
    @DisplayName("updateStatus debe persistir el nuevo estado y publicar OrderStatusChanged")
    void shouldUpdateStatusAndPublishEvent() {
        Order order = new Order();
        order.setId(1L);
        order.setCustomerId("cust-1");
        order.setCustomerName("Juan");
        order.setBranchId(2L);
        order.setStatus(OrderStatus.CREATED);
        order.setDispatchType(DispatchType.PICKUP);
        order.setTotalAmount(new BigDecimal("5000"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.updateStatus(1L, "IN_PREPARATION");

        assertThat(response.getStatus()).isEqualTo("IN_PREPARATION");
        verify(orderRepository).save(order);
        verify(orderStatusChangedPublisher, times(1)).publish(eq(order), eq(OrderStatus.CREATED));
    }

    @Test
    @DisplayName("cancel debe cambiar estado a CANCELLED y publicar OrderStatusChanged")
    void shouldCancelOrderAndPublishEvent() {
        Order order = new Order();
        order.setId(2L);
        order.setCustomerId("cust-2");
        order.setCustomerName("Maria");
        order.setBranchId(1L);
        order.setStatus(OrderStatus.CREATED);
        order.setDispatchType(DispatchType.DELIVERY);
        order.setTotalAmount(new BigDecimal("8000"));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        order.setItems(new ArrayList<>());

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.cancel(2L);

        assertThat(response.getStatus()).isEqualTo("CANCELLED");
        verify(orderRepository).save(order);
        verify(orderStatusChangedPublisher, times(1)).publish(eq(order), eq(OrderStatus.CREATED));
    }
}
