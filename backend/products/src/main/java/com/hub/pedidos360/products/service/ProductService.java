package com.hub.pedidos360.products.service;

import com.hub.pedidos360.products.dto.ProductRequest;
import com.hub.pedidos360.products.dto.ProductResponse;
import com.hub.pedidos360.products.model.Product;
import com.hub.pedidos360.products.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductResponse> getAll() {
        return productRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<ProductResponse> available() {
        return productRepository.findByAvailableTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    public ProductResponse getById(Long id) {
        return toResponse(getByIdEntity(id));
    }

    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setAvailable(request.isAvailable());
        return toResponse(productRepository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getByIdEntity(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setAvailable(request.isAvailable());
        return toResponse(productRepository.save(product));
    }

    /**
     * Descuenta stock de forma atómica (UPDATE condicional en el repositorio).
     * La verificación y el descuento ocurren en una única operación de base de
     * datos, por lo que pedidos simultáneos no dejan el stock en negativo.
     */
    @Transactional
    public ProductResponse decrementStock(Long id, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a 0");
        }

        int updatedRows = productRepository.decrementStockIfAvailable(id, quantity);
        if (updatedRows == 0) {
            Product product = getByIdEntity(id);
            throw new InsufficientStockException(product.getName(), product.getStock(), quantity);
        }

        return getById(id);
    }

    public void delete(Long id) {
        getByIdEntity(id);
        productRepository.deleteById(id);
    }

    private Product getByIdEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory(),
                product.isAvailable());
    }
}