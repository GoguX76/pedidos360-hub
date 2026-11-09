package com.hub.pedidos360.kafka_admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.TopicExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.hub.pedidos360.kafka_admin.config.KafkaAdminProperties;
import com.hub.pedidos360.kafka_admin.dto.CreateTopicRequest;
import com.hub.pedidos360.kafka_admin.dto.UpdateTopicRequest;
import com.hub.pedidos360.kafka_admin.exception.InvalidTopicRequestException;
import com.hub.pedidos360.kafka_admin.exception.TopicAlreadyExistsException;

class KafkaTopicServiceTest {

    private Admin admin;
    private KafkaTopicService service;

    @BeforeEach
    void setUp() {
        admin = mock(Admin.class);
        service = new KafkaTopicService(admin, new KafkaAdminProperties(5, false, List.of()));
    }

    @Test
    @SuppressWarnings("unchecked")
    void crearTopico_enviaParticionesReplicasYConfiguracion() {
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        when(result.all()).thenReturn(KafkaFuture.completedFuture(null));
        when(admin.createTopics(anyCollection())).thenReturn(result);

        service.create(new CreateTopicRequest("pedidos.eventos", 3, (short) 2,
                Map.of("retention.ms", "604800000", "cleanup.policy", "delete")));

        ArgumentCaptor<Collection<NewTopic>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(admin).createTopics(captor.capture());
        NewTopic enviado = captor.getValue().iterator().next();
        assertThat(enviado.name()).isEqualTo("pedidos.eventos");
        assertThat(enviado.numPartitions()).isEqualTo(3);
        assertThat(enviado.replicationFactor()).isEqualTo((short) 2);
        assertThat(enviado.configs()).containsEntry("retention.ms", "604800000");
    }

    @Test
    void crearTopico_configNoPermitida_fallaSinLlamarAKafka() {
        var request = new CreateTopicRequest("x", 1, (short) 1, Map.of("unclean.leader.election.enable", "true"));

        assertThrows(InvalidTopicRequestException.class, () -> service.create(request));
        verify(admin, never()).createTopics(anyCollection());
    }

    @Test
    void crearTopico_yaExiste_lanzaConflicto() {
        CreateTopicsResult result = mock(CreateTopicsResult.class);
        var fallido = new org.apache.kafka.common.internals.KafkaFutureImpl<Void>();
        fallido.completeExceptionally(new TopicExistsException("ya existe"));
        when(result.all()).thenReturn(fallido);
        when(admin.createTopics(anyCollection())).thenReturn(result);

        assertThrows(TopicAlreadyExistsException.class,
                () -> service.create(new CreateTopicRequest("dup", 1, (short) 1, Map.of())));
    }

    @Test
    void actualizar_sinCambios_lanzaError() {
        assertThrows(InvalidTopicRequestException.class,
                () -> service.update("pedidos.eventos", new UpdateTopicRequest(null, null)));
    }

    @Test
    void actualizar_reducirParticiones_noEstaPermitido() {
        Node n = new Node(1, "kafka-1", 29092);
        TopicPartitionInfo p0 = new TopicPartitionInfo(0, n, List.of(n), List.of(n));
        TopicPartitionInfo p1 = new TopicPartitionInfo(1, n, List.of(n), List.of(n));
        TopicPartitionInfo p2 = new TopicPartitionInfo(2, n, List.of(n), List.of(n));
        TopicDescription desc = new TopicDescription("pedidos.eventos", false, List.of(p0, p1, p2));

        DescribeTopicsResult describeResult = mock(DescribeTopicsResult.class);
        when(describeResult.allTopicNames())
                .thenReturn(KafkaFuture.completedFuture(Map.of("pedidos.eventos", desc)));
        when(admin.describeTopics(anyCollection())).thenReturn(describeResult);

        assertThrows(InvalidTopicRequestException.class,
                () -> service.update("pedidos.eventos", new UpdateTopicRequest(2, null)));
        verify(admin, never()).createPartitions(org.mockito.ArgumentMatchers.anyMap());
    }
}
