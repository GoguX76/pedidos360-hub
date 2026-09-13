package com.hub.pedidos360.products.controller;

import com.hub.pedidos360.products.dto.ProductRequest;
import com.hub.pedidos360.products.dto.ProductResponse;
import com.hub.pedidos360.products.model.Product;
import com.hub.pedidos360.products.service.InsufficientStockException;
import com.hub.pedidos360.products.service.ProductNotFoundException;
import com.hub.pedidos360.products.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> list(@RequestParam(defaultValue = "false") boolean onlyAvailable) {
        List<Product> products = onlyAvailable
                ? productService.available()
                : productService.getAll();
        return products.stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return toResponse(productService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return toResponse(productService.create(request));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return toResponse(productService.update(id, request));
    }

    @PostMapping("/{id}/decrement")
    public ProductResponse decrement(@PathVariable Long id, @RequestParam int quantity) {
        return toResponse(productService.decrementStock(id, quantity));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        productService.delete(id);
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

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<String> handleNotFound(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<String> handleInsufficientStock(InsufficientStockException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}