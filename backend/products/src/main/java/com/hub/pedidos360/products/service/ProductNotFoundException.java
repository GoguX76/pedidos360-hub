package com.hub.pedidos360.products.service;

/**
 * Se lanza cuando se busca un producto por id y no existe.
 * El controller la captura para responder HTTP 404 en vez de un
 * error 500 genérico.
 */
public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(Long id) {
        super("No se encontró el producto con id " + id);
    }
}