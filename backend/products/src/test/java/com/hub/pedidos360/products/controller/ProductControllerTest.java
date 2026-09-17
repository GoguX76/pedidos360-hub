package com.hub.pedidos360.products.controller;

import com.hub.pedidos360.products.model.Product;
import com.hub.pedidos360.products.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDb() {
        productRepository.deleteAll();
    }

    @Test
    void getProductsCuandoNoHayNadaDevuelve200YListaVacia() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void createProductValidoDevuelve201() throws Exception {
        String body = """
                {"name":"Arepa Sencilla","description":"De queso amarillo",
                 "price":8.50,"stock":10,"category":"Desayunos","available":true}
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Arepa Sencilla"))
                .andExpect(jsonPath("$.price").value(8.5))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void createProductConNombreVacioDevuelve400ConFieldErrors() throws Exception {
        String body = """
                {"name":"","description":"desc","price":8.50,
                 "stock":10,"category":"Desayunos","available":true}
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void createProductConPrecioNegativoDevuelve400() throws Exception {
        String body = """
                {"name":"Arepa","description":"desc","price":-1,
                 "stock":10,"category":"Desayunos","available":true}
                """;

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.price").exists());
    }

    @Test
    void getByIdInexistenteDevuelve404ConMessage() throws Exception {
        mockMvc.perform(get("/api/v1/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void onlyAvailableFiltraLosNoDisponibles() throws Exception {
        productRepository.save(product("Oculta", false));
        productRepository.save(product("Visible", true));

        mockMvc.perform(get("/api/v1/products").param("onlyAvailable", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Visible"));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void deleteDevuelve204YElProductoDesaparece() throws Exception {
        Product saved = productRepository.save(product("Para borrar", true));

        mockMvc.perform(delete("/api/v1/products/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/products/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void decrementValidoDescuentaElStock() throws Exception {
        Product saved = productRepository.save(product("Arepa", true));

        mockMvc.perform(post("/api/v1/products/" + saved.getId() + "/decrement")
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(8));
    }

    @Test
    void decrementConCantidadInvalidaDevuelve400() throws Exception {
        Product saved = productRepository.save(product("Arepa", true));

        mockMvc.perform(post("/api/v1/products/" + saved.getId() + "/decrement")
                        .param("quantity", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void decrementEnProductoInexistenteDevuelve404() throws Exception {
        mockMvc.perform(post("/api/v1/products/999999/decrement")
                        .param("quantity", "1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").exists());
    }

    private Product product(String name, boolean available) {
        Product p = new Product();
        p.setName(name);
        p.setDescription("desc");
        p.setPrice(new BigDecimal("8.50"));
        p.setStock(10);
        p.setCategory("Desayunos");
        p.setAvailable(available);
        return p;
    }
}