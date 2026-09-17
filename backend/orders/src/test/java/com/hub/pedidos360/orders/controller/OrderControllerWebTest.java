package com.hub.pedidos360.orders.controller;

import tools.jackson.databind.ObjectMapper;
import com.hub.pedidos360.orders.dto.OrderItemRequest;
import com.hub.pedidos360.orders.dto.OrderRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void crearPedido_SinToken_Retorna401() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void listarPedidosDeSucursal_ConToken_Retorna200() throws Exception {
        mockMvc.perform(get("/api/v1/orders/branch/1")
                .with(jwt()))
            .andExpect(status().isOk());
    }

    @Test
    void crearPedido_ConToken_AsignaCustomerIdDelClaimOid() throws Exception {
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Ana");
        request.setBranchId(1L);
        request.setDispatchType("PICKUP");
        request.setItems(List.of(new OrderItemRequest("Arepa", 2, new BigDecimal("5.50"))));

        mockMvc.perform(post("/api/v1/orders")
                .with(jwt().jwt(jwt -> jwt.claim("oid", "oid-123")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.customerId").value("oid-123"))
            .andExpect(jsonPath("$.totalAmount").value(11.00));
    }

    @Test
    void crearPedido_BodyInvalido_Retorna400ConFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"customerName\":\"\",\"items\":[]}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.customerName").exists())
            .andExpect(jsonPath("$.fieldErrors.dispatchType").exists());
    }

    @Test
    void preflightCors_OrigenPermitido_IncluyeHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/orders")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}