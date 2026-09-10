package com.hub.pedidos360.orders.service;

import com.hub.pedidos360.orders.dto.OrderItemRequest;
import com.hub.pedidos360.orders.dto.OrderItemResponse;
import com.hub.pedidos360.orders.dto.OrderRequest;
import com.hub.pedidos360.orders.dto.OrderResponse;
import com.hub.pedidos360.orders.model.DispatchType;
import com.hub.pedidos360.orders.model.Order;
import com.hub.pedidos360.orders.model.OrderItem;
import com.hub.pedidos360.orders.model.OrderStatus;
import com.hub.pedidos360.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Crea el pedido del cliente, obteniendo los datos del modelo Order,
     * haciendo el cálculo del total del pedido y parseando el JSON para
     * asegurar seguridad y funcionalidad del método
     */
    public OrderResponse create(OrderRequest request) {
        DispatchType dispatchType = parseDispatchType(request.getDispatchType());

        Order order = new Order();
        order.setCustomerId("pending");
        order.setCustomerName(request.getCustomerName());
        order.setBranchId(request.getBranchId());
        order.setDispatchType(dispatchType);
        order.setStatus(OrderStatus.CREATED);

        BigDecimal total = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {
            OrderItem item = new OrderItem();
            item.setProductName(itemRequest.getProductName());
            item.setQuantity(itemRequest.getQuantity());
            item.setUnitPrice(BigDecimal.valueOf(itemRequest.getUnitPrice()));
            item.setOrder(order);

            order.getItems().add(item);

            BigDecimal itemTotal = item.getUnitPrice()
                .multiply(BigDecimal.valueOf(item.getQuantity()));
            total = total.add(itemTotal);
        }

        order.setTotalAmount(total);

        Order saved = orderRepository.save(order);
        return toResponse(saved);
    }

    /**
     * Permite encontrar un pedido mediante el ID asociado
     */

    public OrderResponse findById(Long id) {
        Order order = orderRepository.findById(id)
            .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    /**
     * Permite encontrar un pedido de un cliente mediante el
     * ID del mismo, esto sirve para que el mismo cliente pueda
     * ver los pedidos que tiene asociados
     */

    public List<OrderResponse> findByCustomer(String customerId) {
        return orderRepository.findByCustomerId(customerId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Permite encontrar un pedido mediante el ID del local al
     * que pertenece
     */

    public List<OrderResponse> findByBranch(Long branchId) {
        return orderRepository.findByBranchId(branchId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Permite encontrar un pedido por el estado que posee. Con esto
     * es posible filtrar los pedidos para visualizar los que están
     * pendientes, entregados o cancelados
     */
    public List<OrderResponse> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    /**
     * Lanza error cuando el valor de tipo de despacho no corresponde
     * a los especificados, validando que sólo sean DELIVERY (Entrega)
     * o PICKUP (Retiro)
     */
    private DispatchType parseDispatchType(String value) {
        try {
            return DispatchType.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "Invalid dispatch type: " + value
                + ". Allowed values: DELIVERY, PICKUP");
        }
    }

    /**
     * Transforma la entidad de Order en la BD a DTO, con el propósito
     * de devolver un JSON limpio que no entre en bucle por las referencias
     * entre Orden y OrdenItem
     */
    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
            .map(item -> new OrderItemResponse(
                item.getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice()))
            .toList();

        return new OrderResponse(
            order.getId(),
            order.getCustomerId(),
            order.getCustomerName(),
            order.getBranchId(),
            order.getStatus().name(),
            order.getDispatchType().name(),
            order.getTotalAmount(),
            order.getCreatedAt(),
            order.getUpdatedAt(),
            items);
    }
}
