package com.hub.pedidos360.products.controller;

import com.hub.pedidos360.products.dto.ProductRequest;
import com.hub.pedidos360.products.model.Product;
import com.hub.pedidos360.products.service.ProductNotFoundException;
import com.hub.pedidos360.products.service.ProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Expone el catálogo de productos por HTTP. No contiene lógica de
 * negocio — solo recibe la petición, se la pasa al service, y arma
 * la respuesta HTTP correspondiente.
 */
@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** GET /products?soloDisponibles=true */
    @GetMapping
    public List<Product> listar(@RequestParam(defaultValue = "false") boolean soloDisponibles) {
        return soloDisponibles
                ? productService.obtenerDisponibles()
                : productService.obtenerTodos();
    }

    /** GET /products/{id} */
    @GetMapping("/{id}")
    public Product obtenerUno(@PathVariable Long id) {
        return productService.obtenerPorId(id);
    }

    /** POST /products */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Product crear(@RequestBody ProductRequest request) {
        return productService.crear(request.getNombre(), request.getPrecio(), request.isDisponible());
    }

    /** PUT /products/{id} */
    @PutMapping("/{id}")
    public Product actualizar(@PathVariable Long id, @RequestBody ProductRequest request) {
        return productService.actualizar(id, request.getNombre(), request.getPrecio(), request.isDisponible());
    }

    /** DELETE /products/{id} */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        productService.eliminar(id);
    }

    /**
     * Se ejecuta automáticamente cada vez que se lanza un
     * ProductNotFoundException en cualquier método de arriba.
     * Aquí es donde esa excepción se convierte en un HTTP 404 real.
     */
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<String> manejarNoEncontrado(ProductNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }
}