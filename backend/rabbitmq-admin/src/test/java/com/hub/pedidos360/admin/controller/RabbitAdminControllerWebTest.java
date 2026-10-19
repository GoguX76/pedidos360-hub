package com.hub.pedidos360.admin.controller;

import com.hub.pedidos360.admin.dto.AdminActionResponse;
import com.hub.pedidos360.admin.service.RabbitAdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RabbitAdminControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RabbitAdminService rabbitAdminService;

    private AdminActionResponse okResponse(String type, String name) {
        return AdminActionResponse.builder()
            .success(true)
            .message("OK")
            .resourceType(type)
            .resourceName(name)
            .timestamp(Instant.now())
            .build();
    }

    @Test
    void crearCola_SinToken_Retorna401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/queues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"q.test\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void crearCola_ConToken_Retorna201() throws Exception {
        when(rabbitAdminService.createQueue(any())).thenReturn(okResponse("QUEUE", "q.test"));

        mockMvc.perform(post("/api/v1/admin/queues")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"q.test\",\"durable\":true}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.resourceType").value("QUEUE"));
    }

    @Test
    void crearExchange_ConToken_Retorna201() throws Exception {
        when(rabbitAdminService.createExchange(any())).thenReturn(okResponse("EXCHANGE", "test.exchange"));

        mockMvc.perform(post("/api/v1/admin/exchanges")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test.exchange\",\"type\":\"topic\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.resourceType").value("EXCHANGE"));
    }

    @Test
    void crearCola_BodyInvalido_Retorna400ConFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/admin/queues")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.name").exists());
    }

    @Test
    void crearExchange_TipoInvalido_Retorna400() throws Exception {
        when(rabbitAdminService.createExchange(any()))
            .thenThrow(new IllegalArgumentException("Tipo de exchange no soportado"));

        mockMvc.perform(post("/api/v1/admin/exchanges")
                .with(jwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"test.exc\",\"type\":\"inventado\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarBinding_ConToken_Retorna200() throws Exception {
        when(rabbitAdminService.deleteBinding(any())).thenReturn(okResponse("BINDING", "q.test -> test.exchange"));

        mockMvc.perform(delete("/api/v1/admin/bindings/q.test/test.exchange/test.key")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.resourceType").value("BINDING"));
    }

    @Test
    void preflightCors_OrigenPermitido_IncluyeHeaders() throws Exception {
        mockMvc.perform(options("/api/v1/admin/queues")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"))
            .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
