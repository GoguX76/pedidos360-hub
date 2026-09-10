package com.hub.pedidos360.products.dto;

/**
 * Lo que el cliente envía al crear o actualizar un producto.
 * Separado del model porque el cliente no debería poder mandar
 * un "id" propio — ese lo genera la base de datos.
 */
public class ProductRequest {

    private String nombre;
    private Double precio;
    private boolean disponible;

    protected ProductRequest() {
    }

    public String getNombre() {
        return nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public boolean isDisponible() {
        return disponible;
    }
}