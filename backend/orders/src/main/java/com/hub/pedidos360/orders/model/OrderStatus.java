package com.hub.pedidos360.orders.model;

// Enumera los diferentes tipos de estado en el que puede estar un pedido
public enum OrderStatus {
    CREATED,
    IN_PREPARATION,
    READY,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}
