package com.hub.pedidos360.products.service;

import com.hub.pedidos360.products.model.Product;
import com.hub.pedidos360.products.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Lógica de negocio del catálogo de productos. El controller no habla
 * directo con el repository — todo pasa por aquí, para que las reglas
 * de negocio (como qué pasa si no se encuentra un producto) vivan en
 * un solo lugar.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Todos los productos, disponibles o no (para el panel de administración). */
    public List<Product> obtenerTodos() {
        return productRepository.findAll();
    }

    /** Solo los productos disponibles (para mostrar en el menú al cliente). */
    public List<Product> obtenerDisponibles() {
        return productRepository.findByDisponibleTrue();
    }

    /**
     * Busca un producto por id.
     *
     * @throws ProductNotFoundException si no existe, para que el
     * controller pueda responder un 404 en vez de un error genérico.
     */
    public Product obtenerPorId(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product crear(String nombre, Double precio, boolean disponible) {
        Product product = new Product(nombre, precio, disponible);
        return productRepository.save(product);
    }

    public Product actualizar(Long id, String nombre, Double precio, boolean disponible) {
        Product product = obtenerPorId(id);
        product.setNombre(nombre);
        product.setPrecio(precio);
        product.setDisponible(disponible);
        return productRepository.save(product);
    }

    public void eliminar(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}