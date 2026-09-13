package com.hub.pedidos360.products.service;

import com.hub.pedidos360.products.dto.ProductRequest;
import com.hub.pedidos360.products.model.Product;
import com.hub.pedidos360.products.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<Product> getAll() {
        return productRepository.findAll();
    }

    public List<Product> available() {
        return productRepository.findByAvailableTrue();
    }

    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    public Product create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setAvailable(request.isAvailable());
        return productRepository.save(product);
}

    public Product update(Long id, ProductRequest request) {
        Product product = getById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(request.getCategory());
        product.setAvailable(request.isAvailable());
        return productRepository.save(product);
    }

    public Product decrementStock(Long id, int quantity) {
        Product product = getById(id);
        if (product.getStock() < quantity) {
            throw new InsufficientStockException(product.getName(), product.getStock(), quantity);
        }
        product.setStock(product.getStock() - quantity);
        return productRepository.save(product);
    }

    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }
}