package com.hub.pedidos360.orders.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem {
    
    // Define ID auto-incrementable para la BD
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nombre del producto
    @Column(nullable = false)
    private String productName;

    // Cantidad del producto
    @Column(nullable = false)
    private Integer quantity;

    // Precio unitario del producto
    @Column(nullable = false)
    private BigDecimal unitPrice;

    // Hacemos la relación de muchos productos para un pedido, indicando el id del pedido
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
}
