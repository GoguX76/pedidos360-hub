package com.hub.pedidos360.orders.service;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long id) {
        super("No se encontró el pedido con id " + id);
    }
}
