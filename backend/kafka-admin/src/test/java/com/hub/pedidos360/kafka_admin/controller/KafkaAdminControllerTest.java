package com.hub.pedidos360.kafka_admin.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.hub.pedidos360.kafka_admin.dto.ConsumerGroupInfo;
import com.hub.pedidos360.kafka_admin.dto.TopicInfo;
import com.hub.pedidos360.kafka_admin.exception.GlobalExceptionHandler;
import com.hub.pedidos360.kafka_admin.exception.KafkaAdminException;
import com.hub.pedidos360.kafka_admin.exception.TopicNotFoundException;
import com.hub.pedidos360.kafka_admin.service.KafkaConsumerGroupService;
import com.hub.pedidos360.kafka_admin.service.KafkaTopicService;

/** Prueba el controller y el manejo de errores sin levantar Spring ni Kafka. */
class KafkaAdminControllerTest {

    private KafkaTopicService topics;
    private KafkaConsumerGroupService groups;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        topics = mock(KafkaTopicService.class);
        groups = mock(KafkaConsumerGroupService.class);
        mvc = MockMvcBuilders.standaloneSetup(new KafkaAdminController(topics, groups))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listarTopicos_devuelve200() throws Exception {
        when(topics.list()).thenReturn(List.of(new TopicInfo("pedidos.eventos", 3, 2, false)));

        mvc.perform(get("/api/kafka/topics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("pedidos.eventos"))
                .andExpect(jsonPath("$[0].partitions").value(3));
    }

    @Test
    void crearTopico_valido_devuelve201() throws Exception {
        when(topics.create(any())).thenReturn(new TopicInfo("nuevo", 2, 1, false));

        mvc.perform(post("/api/kafka/topics").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"nuevo\",\"partitions\":2,\"replicationFactor\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("nuevo"));
    }

    @Test
    void crearTopico_nombreInvalido_devuelve400() throws Exception {
        mvc.perform(post("/api/kafka/topics").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"nombre con espacios\",\"partitions\":2,\"replicationFactor\":1}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void eliminarTopico_devuelve204() throws Exception {
        mvc.perform(delete("/api/kafka/topics/viejo")).andExpect(status().isNoContent());
        verify(topics).delete("viejo");
    }

    @Test
    void topicoInexistente_devuelve404() throws Exception {
        when(topics.get("nada")).thenThrow(new TopicNotFoundException("no existe"));

        mvc.perform(get("/api/kafka/topics/nada"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("no existe"));
    }

    @Test
    void kafkaCaido_devuelve502() throws Exception {
        when(groups.list()).thenThrow(new KafkaAdminException("Kafka no respondio"));

        mvc.perform(get("/api/kafka/consumer-groups")).andExpect(status().isBadGateway());
    }

    @Test
    void listarGrupos_devuelve200() throws Exception {
        when(groups.list()).thenReturn(List.of(new ConsumerGroupInfo("inventario-group", "STABLE")));

        mvc.perform(get("/api/kafka/consumer-groups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].groupId").value("inventario-group"));
    }
}
