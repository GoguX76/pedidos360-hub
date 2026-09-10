package com.hub.pedidos360.products.model;

import jakarta.persistence.*;

/**
 * Representa un producto del catálogo (ej. un plato o bebida que se
 * puede pedir). Es una entidad de base de datos porque el catálogo
 * necesita persistir entre reinicios del servicio.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private Double precio;

    @Column(nullable = false)
    private boolean disponible = true;

    // JPA exige un constructor vacío.
    protected Product() {
    }

    public Product(String nombre, Double precio, boolean disponible) {
        this.nombre = nombre;
        this.precio = precio;
        this.disponible = disponible;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Double getPrecio() {
        return precio;
    }

    public void setPrecio(Double precio) {
        this.precio = precio;
    }

    public boolean isDisponible() {
        return disponible;
    }

    public void setDisponible(boolean disponible) {
        this.disponible = disponible;
    }
}