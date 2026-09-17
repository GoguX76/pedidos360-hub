package com.hub.pedidos360.orders.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hub.pedidos360.orders.dto.OrderRequest;
import com.hub.pedidos360.orders.dto.OrderResponse;
import com.hub.pedidos360.orders.dto.OrderStatusRequest;
import com.hub.pedidos360.orders.model.OrderStatus;
import com.hub.pedidos360.orders.service.OrderService;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /**
     * Devuelve los datos de un pedido mediante su ID. Ideal para verificar la existencia
     * del pedido y visualizarlo en el KDS
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    /**
     * Devuelve los datos de pedidos asociados a un cliente mediante su ID. Ideal para
     * que el cliente pueda ver que pedidos están vinculados a su cuenta
     */
    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> findByCustomer(@PathVariable String customerId) {
        return ResponseEntity.ok(orderService.findByCustomer(customerId));
    }

    /**
     * Devuelve los datos de pedidos asociados a una sucursal mediante su ID. Ideal para
     * ver los pedidos más concurrentes de un local
     */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<OrderResponse>> findByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(orderService.findByBranch(branchId));
    }

    /**
     * Devuelve los datos de pedidos que tengan un estado especifico. Ideal para ver cuantos
     * pedidos han sido entregados, cancelados, pendientes, etc
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<OrderResponse>> findByStatus(@PathVariable OrderStatus status) {
        return ResponseEntity.ok(orderService.findByStatus(status));
    }

    /**
     * Permite crear un pedido en el sistema
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OrderRequest request) {
        String customerId = jwt.getClaimAsString("oid");
        OrderResponse response = orderService.create(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Actualiza el estado de un pedido mediante su ID. Ideal para cuando el pedido está
     * listo para repartir, fue cancelado, etc
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateStatus(id, request.getStatus()));
    }

    /**
     * Permite eliminar un pedido mediante su ID. Ideal si el pedido tiene de estado
     * cancelado o entregado por tiempo prolongado
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<OrderResponse> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancel(id));
    }
}
