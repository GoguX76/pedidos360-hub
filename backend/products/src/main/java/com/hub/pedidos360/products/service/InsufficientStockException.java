package com.hub.pedidos360.products.service;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productName, int stock, int requested) {
        super("Stock insuficiente para el producto " + productName
                + ". Disponible: " + stock + ", solicitado: " + requested);
    }
}